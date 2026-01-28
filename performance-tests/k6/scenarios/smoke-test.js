import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { SharedArray } from 'k6/data';
import { BASE_URL, ENDPOINTS, HEADERS, DEFAULT_REGION } from '../lib/config.js';
import { validateResponse, validateApiSuccess, customMetrics } from '../lib/helpers.js';
import { smokeThresholds } from '../thresholds.js';

/**
 * Smoke Test
 *
 * 목적: 기본 기능 동작 확인
 * 설정: 5 VUs, 1분
 * 대상: 모든 주요 API 엔드포인트 1회씩 호출
 */

// 테스트 데이터 로드
const testSummoners = new SharedArray('summoners', function () {
    return JSON.parse(open('../../data/test-summoners.json'));
});

// k6 옵션
export const options = {
    vus: 5,
    duration: '1m',
    thresholds: smokeThresholds,
    tags: {
        testType: 'smoke',
    },
};

// 요청 옵션
const requestOptions = {
    headers: HEADERS,
    timeout: '30s',
};

export default function () {
    const summoner = testSummoners[__VU % testSummoners.length];
    const region = summoner.region || DEFAULT_REGION;
    const gameName = summoner.gameName;
    let puuid = summoner.puuid;

    // 1. Champion Rotation (캐시 API - 가장 빠름)
    group('Champion Rotation', function () {
        const url = `${BASE_URL}${ENDPOINTS.champion.rotation(region)}`;
        const response = http.get(url, requestOptions);

        customMetrics.championRotationResponseTime.add(response.timings.duration);

        const isValid = validateResponse(response, 'Champion Rotation');
        customMetrics.championRotationSuccessRate.add(isValid);

        if (isValid) {
            validateApiSuccess(response, 'Champion Rotation');
        }
    });

    sleep(0.5);

    // 2. Autocomplete (DB LIKE 쿼리)
    group('Autocomplete Search', function () {
        const searchQuery = gameName.substring(0, 2);
        const url = `${BASE_URL}${ENDPOINTS.summoner.autocomplete(searchQuery, region)}`;
        const response = http.get(url, requestOptions);

        customMetrics.autocompleteResponseTime.add(response.timings.duration);

        const isValid = validateResponse(response, 'Autocomplete');
        customMetrics.autocompleteSuccessRate.add(isValid);

        if (isValid) {
            validateApiSuccess(response, 'Autocomplete');
        }
    });

    sleep(0.5);

    // 3. Summoner Detail (DB + 외부 API)
    group('Summoner Detail', function () {
        const url = `${BASE_URL}${ENDPOINTS.summoner.getByGameName(region, gameName)}`;
        const response = http.get(url, requestOptions);

        customMetrics.summonerResponseTime.add(response.timings.duration);

        const isValid = validateResponse(response, 'Summoner Detail');
        customMetrics.summonerSuccessRate.add(isValid);

        if (isValid) {
            const apiSuccess = validateApiSuccess(response, 'Summoner Detail');
            if (apiSuccess) {
                // puuid 추출 (이후 API에서 사용)
                try {
                    const body = JSON.parse(response.body);
                    puuid = body.data?.puuid || puuid;
                } catch (e) {
                    console.warn('Failed to extract puuid from response');
                }
            }
        }
    });

    sleep(0.5);

    // puuid가 있어야 진행 가능한 API들
    if (puuid) {
        // 4. League Info (단일 조회)
        group('League Info', function () {
            const url = `${BASE_URL}${ENDPOINTS.league.getByPuuid(puuid)}`;
            const response = http.get(url, requestOptions);

            customMetrics.leagueResponseTime.add(response.timings.duration);

            const isValid = validateResponse(response, 'League Info');
            customMetrics.leagueSuccessRate.add(isValid);

            if (isValid) {
                validateApiSuccess(response, 'League Info');
            }
        });

        sleep(0.5);

        // 5. Match List (페이징)
        group('Match List', function () {
            const url = `${BASE_URL}${ENDPOINTS.match.getMatches(puuid, 0, 10)}`;
            const response = http.get(url, requestOptions);

            customMetrics.matchResponseTime.add(response.timings.duration);

            const isValid = validateResponse(response, 'Match List');
            customMetrics.matchSuccessRate.add(isValid);

            if (isValid) {
                validateApiSuccess(response, 'Match List');
            }
        });

        sleep(0.5);

        // 6. Rank Champions (통계 집계)
        group('Rank Champions', function () {
            const url = `${BASE_URL}${ENDPOINTS.rank.champions(puuid, 420)}`;
            const response = http.get(url, requestOptions);

            customMetrics.rankChampionsResponseTime.add(response.timings.duration);

            const isValid = validateResponse(response, 'Rank Champions');
            customMetrics.rankChampionsSuccessRate.add(isValid);

            if (isValid) {
                validateApiSuccess(response, 'Rank Champions');
            }
        });
    } else {
        console.warn(`Skipping puuid-dependent APIs - no puuid for ${gameName}`);
    }

    sleep(1);
}

// 테스트 종료 후 요약
export function handleSummary(data) {
    console.log('\n=== Smoke Test Summary ===');
    console.log(`Total Requests: ${data.metrics.http_reqs?.values?.count || 0}`);
    console.log(`Failed Requests: ${data.metrics.http_req_failed?.values?.rate * 100 || 0}%`);
    console.log(`Avg Duration: ${data.metrics.http_req_duration?.values?.avg?.toFixed(2) || 0}ms`);
    console.log(`P95 Duration: ${data.metrics.http_req_duration?.values?.['p(95)']?.toFixed(2) || 0}ms`);

    return {
        'stdout': JSON.stringify(data, null, 2),
    };
}
