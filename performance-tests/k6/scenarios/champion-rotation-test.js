import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { BASE_URL, ENDPOINTS, HEADERS } from '../lib/config.js';
import { customMetrics } from '../lib/helpers.js';
import { championRotationThresholds } from '../thresholds.js';

/**
 * Champion Rotation API 전용 성능 테스트
 *
 * 특성:
 * - Redis 캐시 기반 (1시간 TTL)
 * - 빠른 응답 기대 (p95 < 100ms)
 * - 높은 안정성 (에러율 < 0.01%)
 *
 * 테스트 시나리오:
 * 1. cache_hit_load: 캐시 히트 부하 테스트 (100 VUs, 2분)
 * 2. multi_region: 멀티 리전 동시 호출 (50 VUs, 3분)
 * 3. spike: 스파이크 테스트 (10 → 200 → 10 VUs)
 */

// 테스트 대상 리전
const REGIONS = ['kr', 'na1', 'euw1', 'eun1', 'jp1'];

// 시나리오별 커스텀 메트릭
const cacheHitResponseTime = new Trend('cache_hit_response_time', true);
const cacheHitSuccessRate = new Rate('cache_hit_success_rate');
const multiRegionResponseTime = new Trend('multi_region_response_time', true);
const multiRegionSuccessRate = new Rate('multi_region_success_rate');
const spikeResponseTime = new Trend('spike_response_time', true);
const spikeSuccessRate = new Rate('spike_success_rate');

// 요청 옵션
const requestOptions = {
    headers: HEADERS,
    timeout: '30s',
    tags: { name: 'ChampionRotation' },
};

// k6 옵션
export const options = {
    scenarios: {
        // 1. 캐시 히트 부하 테스트
        cache_hit_load: {
            executor: 'constant-vus',
            vus: 100,
            duration: '2m',
            tags: { scenario: 'cache_hit' },
            exec: 'cacheHitTest',
        },
        // 2. 멀티 리전 테스트
        multi_region: {
            executor: 'constant-vus',
            vus: 50,
            duration: '3m',
            startTime: '2m',
            tags: { scenario: 'multi_region' },
            exec: 'multiRegionTest',
        },
        // 3. 스파이크 테스트
        spike: {
            executor: 'ramping-vus',
            stages: [
                { duration: '10s', target: 10 },   // 워밍업
                { duration: '10s', target: 200 },  // 급격한 증가
                { duration: '30s', target: 200 },  // 최대 부하 유지
                { duration: '10s', target: 10 },   // 급격한 감소
            ],
            startTime: '5m',
            tags: { scenario: 'spike' },
            exec: 'spikeTest',
        },
    },
    thresholds: championRotationThresholds,
    tags: {
        testType: 'champion-rotation',
    },
};

/**
 * Champion Rotation API 호출 및 검증
 * @param {string} region - 리전 코드
 * @param {Trend} responseTimeTrend - 응답 시간 메트릭
 * @param {Rate} successRate - 성공률 메트릭
 * @returns {boolean} 성공 여부
 */
function callChampionRotation(region, responseTimeTrend, successRate) {
    const url = `${BASE_URL}${ENDPOINTS.champion.rotation(region)}`;
    const response = http.get(url, {
        ...requestOptions,
        tags: { ...requestOptions.tags, region: region },
    });

    // 응답 시간 기록
    responseTimeTrend.add(response.timings.duration);
    customMetrics.championRotationResponseTime.add(response.timings.duration);

    // 응답 검증
    const isValidStatus = check(response, {
        'status is 200': (r) => r.status === 200,
    });

    let isValidBody = false;
    if (isValidStatus) {
        try {
            const body = JSON.parse(response.body);
            isValidBody = check(response, {
                'success is true': () => body.success === true,
                'freeChampionIds exists': () => Array.isArray(body.data?.freeChampionIds),
                'maxNewPlayerLevel > 0': () => body.data?.maxNewPlayerLevel > 0,
            });
        } catch (e) {
            console.error(`[${region}] Failed to parse response: ${e.message}`);
        }
    }

    const isSuccess = isValidStatus && isValidBody;
    successRate.add(isSuccess);
    customMetrics.championRotationSuccessRate.add(isSuccess);

    if (!isSuccess) {
        console.error(`[${region}] Request failed - Status: ${response.status}, Duration: ${response.timings.duration}ms`);
    }

    return isSuccess;
}

/**
 * 시나리오 1: 캐시 히트 부하 테스트
 * 단일 리전(kr)에 집중하여 캐시 성능 측정
 */
export function cacheHitTest() {
    group('Cache Hit Load Test', function () {
        callChampionRotation('kr', cacheHitResponseTime, cacheHitSuccessRate);
    });
    sleep(0.1); // 초당 약 10회 요청
}

/**
 * 시나리오 2: 멀티 리전 동시 호출 테스트
 * 여러 리전에 라운드로빈으로 요청
 */
export function multiRegionTest() {
    group('Multi-Region Test', function () {
        const region = REGIONS[__VU % REGIONS.length];
        callChampionRotation(region, multiRegionResponseTime, multiRegionSuccessRate);
    });
    sleep(0.2); // 초당 약 5회 요청
}

/**
 * 시나리오 3: 스파이크 테스트
 * 급격한 트래픽 증가/감소 대응 능력 테스트
 */
export function spikeTest() {
    group('Spike Test', function () {
        // 스파이크 테스트에서는 랜덤 리전 선택
        const region = REGIONS[Math.floor(Math.random() * REGIONS.length)];
        callChampionRotation(region, spikeResponseTime, spikeSuccessRate);
    });
    sleep(0.05); // 더 빠른 요청 속도
}

/**
 * 기본 함수 (시나리오 미지정 시 실행)
 */
export default function () {
    cacheHitTest();
}

/**
 * 테스트 시작 전 설정
 */
export function setup() {
    console.log('=== Champion Rotation Performance Test ===');
    console.log(`Target URL: ${BASE_URL}`);
    console.log(`Test Regions: ${REGIONS.join(', ')}`);
    console.log('');

    // 워밍업: 각 리전별 1회 요청
    console.log('Warming up cache...');
    for (const region of REGIONS) {
        const url = `${BASE_URL}${ENDPOINTS.champion.rotation(region)}`;
        const response = http.get(url, requestOptions);
        console.log(`  ${region}: ${response.status} (${response.timings.duration.toFixed(2)}ms)`);
    }
    console.log('Warmup complete.\n');

    return { startTime: new Date().toISOString() };
}

/**
 * 테스트 종료 후 요약
 */
export function handleSummary(data) {
    const summary = {
        testType: 'Champion Rotation Performance Test',
        totalRequests: data.metrics.http_reqs?.values?.count || 0,
        failedRate: ((data.metrics.http_req_failed?.values?.rate || 0) * 100).toFixed(4) + '%',
        scenarios: {
            cacheHit: {
                avgDuration: data.metrics.cache_hit_response_time?.values?.avg?.toFixed(2) + 'ms',
                p95Duration: data.metrics.cache_hit_response_time?.values?.['p(95)']?.toFixed(2) + 'ms',
                successRate: ((data.metrics.cache_hit_success_rate?.values?.rate || 0) * 100).toFixed(2) + '%',
            },
            multiRegion: {
                avgDuration: data.metrics.multi_region_response_time?.values?.avg?.toFixed(2) + 'ms',
                p95Duration: data.metrics.multi_region_response_time?.values?.['p(95)']?.toFixed(2) + 'ms',
                successRate: ((data.metrics.multi_region_success_rate?.values?.rate || 0) * 100).toFixed(2) + '%',
            },
            spike: {
                avgDuration: data.metrics.spike_response_time?.values?.avg?.toFixed(2) + 'ms',
                p95Duration: data.metrics.spike_response_time?.values?.['p(95)']?.toFixed(2) + 'ms',
                successRate: ((data.metrics.spike_success_rate?.values?.rate || 0) * 100).toFixed(2) + '%',
            },
        },
    };

    console.log('\n=== Champion Rotation Test Summary ===');
    console.log(`Total Requests: ${summary.totalRequests}`);
    console.log(`Failed Rate: ${summary.failedRate}`);
    console.log('\nScenario Results:');
    console.log('  Cache Hit:');
    console.log(`    Avg: ${summary.scenarios.cacheHit.avgDuration}, P95: ${summary.scenarios.cacheHit.p95Duration}`);
    console.log(`    Success Rate: ${summary.scenarios.cacheHit.successRate}`);
    console.log('  Multi-Region:');
    console.log(`    Avg: ${summary.scenarios.multiRegion.avgDuration}, P95: ${summary.scenarios.multiRegion.p95Duration}`);
    console.log(`    Success Rate: ${summary.scenarios.multiRegion.successRate}`);
    console.log('  Spike:');
    console.log(`    Avg: ${summary.scenarios.spike.avgDuration}, P95: ${summary.scenarios.spike.p95Duration}`);
    console.log(`    Success Rate: ${summary.scenarios.spike.successRate}`);

    return {
        'stdout': JSON.stringify(summary, null, 2),
    };
}
