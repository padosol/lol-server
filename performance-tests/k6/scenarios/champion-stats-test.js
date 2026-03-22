import http from 'k6/http';
import { check } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

import { BASE_URL, DEFAULT_PLATFORM_ID, ENDPOINTS, HEADERS } from '../lib/config.js';
import { customMetrics, randomItem, randomInt } from '../lib/helpers.js';

/**
 * Champion Stats 캐시 on/off 성능 비교 테스트
 *
 * 대상 API:
 *   - GET /api/v1/{platformId}/champion-stats?championId=&patch=&tier=
 *   - GET /api/v1/{platformId}/champion-stats/positions?patch=&tier=
 *
 * 목적: Redis 캐시 도입 전후 응답시간/처리량 비교
 *
 * 실행:
 *   # 캐시 OFF
 *   k6 run -e PHASE=cache-off scenarios/champion-stats-test.js
 *
 *   # 캐시 ON
 *   k6 run -e PHASE=cache-on scenarios/champion-stats-test.js
 *
 *   # 결과: results/champion-stats-{phase}-{timestamp}.json / .txt
 */

// 인기 챔피언 ID 목록
const CHAMPION_IDS = [
    157, 238, 64, 412, 222,   // Yasuo, Zed, Lee Sin, Thresh, Jinx
    103, 202, 236, 53, 555,   // Ahri, Jhin, Lucian, Blitzcrank, Pyke
    39, 91, 145, 498, 497,    // Irelia, Talon, Kai'Sa, Xayah, Rakan
    13, 1, 266, 86, 122,      // Ryze, Annie, Aatrox, Garen, Darius
];

const TIERS = ['EMERALD', 'DIAMOND', 'MASTER', 'GRANDMASTER', 'CHALLENGER'];

const platformId = __ENV.PLATFORM_ID || DEFAULT_PLATFORM_ID;
const patch = __ENV.PATCH || '16.5';
const tier = __ENV.TIER || 'GRANDMASTER';
const phase = __ENV.PHASE || 'unknown';

export const options = {
    scenarios: {
        champion_stats_load: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.RATE || 50),
            timeUnit: '1s',
            duration: __ENV.DURATION || '1m',
            preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 50),
            maxVUs: Number(__ENV.MAX_VUS || 100),
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        champion_stats_detail_response_time: ['p(95)<500', 'avg<300'],
        champion_stats_detail_success_rate: ['rate>0.995'],
        champion_stats_positions_response_time: ['p(95)<500', 'avg<300'],
        champion_stats_positions_success_rate: ['rate>0.995'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
    tags: {
        testType: 'load',
        scenario: 'champion-stats-test',
        phase: phase,
    },
};

const requestOptions = {
    headers: HEADERS,
    timeout: '30s',
};

export default function () {

    callDetailApi();
    // 70% detail API, 30% positions API
    // if (Math.random() < 0.7) {
    //     callDetailApi();
    // } else {
    //     callPositionsApi();
    // }
}

function callDetailApi() {
    const championId = randomItem(CHAMPION_IDS);
    const selectedTier = randomItem(TIERS);
    const endpoint = ENDPOINTS.championStats.getDetail(platformId, championId, patch, selectedTier);
    const url = `${BASE_URL}${endpoint}`;

    const response = http.get(url, requestOptions);

    customMetrics.championStatsDetailResponseTime.add(response.timings.duration);

    const basicChecks = check(response, {
        'detail: status is 200': (r) => r.status === 200,
        'detail: response time < 5s': (r) => r.timings.duration < 5000,
        'detail: response body exists': (r) => r.body && r.body.length > 0,
    });

    let apiSuccess = false;

    if (response.status === 200) {
        try {
            const body = JSON.parse(response.body);
            apiSuccess = body.result === 'SUCCESS';

            check(response, {
                'detail: API success': () => apiSuccess,
            });
        } catch (error) {
            console.error(`[detail] Failed to parse response for championId=${championId}: ${error.message}`);
        }
    } else {
        console.error(
            `[detail] Request failed status=${response.status} duration=${response.timings.duration}ms championId=${championId} url=${url}`
        );
    }

    const success = basicChecks && apiSuccess;
    customMetrics.championStatsDetailSuccessRate.add(success);

    if (!success) {
        customMetrics.errorCount.add(1);
    }
}

function callPositionsApi() {
    const selectedTier = randomItem(TIERS);
    const endpoint = ENDPOINTS.championStats.getPositions(platformId, patch, selectedTier);
    const url = `${BASE_URL}${endpoint}`;

    const response = http.get(url, requestOptions);

    customMetrics.championStatsPositionsResponseTime.add(response.timings.duration);

    const basicChecks = check(response, {
        'positions: status is 200': (r) => r.status === 200,
        'positions: response time < 5s': (r) => r.timings.duration < 5000,
        'positions: response body exists': (r) => r.body && r.body.length > 0,
    });

    let apiSuccess = false;

    if (response.status === 200) {
        try {
            const body = JSON.parse(response.body);
            apiSuccess = body.result === 'SUCCESS';

            check(response, {
                'positions: API success': () => apiSuccess,
            });
        } catch (error) {
            console.error(`[positions] Failed to parse response: ${error.message}`);
        }
    } else {
        console.error(
            `[positions] Request failed status=${response.status} duration=${response.timings.duration}ms url=${url}`
        );
    }

    const success = basicChecks && apiSuccess;
    customMetrics.championStatsPositionsSuccessRate.add(success);

    if (!success) {
        customMetrics.errorCount.add(1);
    }
}

// --- handleSummary: 테스트 종료 시 결과 자동 저장 ---

function round(val) {
    return Math.round((val || 0) * 100) / 100;
}

function extractTrendMetrics(metric) {
    if (!metric) return null;
    const v = metric.values;
    return {
        avg_ms: round(v?.avg),
        med_ms: round(v?.med),
        p95_ms: round(v?.['p(95)']),
        p99_ms: round(v?.['p(99)']),
        max_ms: round(v?.max),
        min_ms: round(v?.min),
    };
}

function extractRateMetrics(metric) {
    if (!metric) return null;
    const v = metric.values;
    return {
        success_rate: round(v?.rate),
        passes: v?.passes || 0,
        fails: v?.fails || 0,
    };
}

function formatApiSection(name, trend, rate) {
    if (!trend) return `[${name}]\n  데이터 없음\n`;
    const successPct = ((rate?.success_rate || 0) * 100).toFixed(2);
    return `[${name}]\n` +
        `  avg: ${trend.avg_ms}ms  |  p95: ${trend.p95_ms}ms  |  p99: ${trend.p99_ms}ms\n` +
        `  Success Rate: ${successPct}% (${rate?.passes || 0} passed / ${rate?.fails || 0} failed)\n`;
}

export function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
    const baseName = `champion-stats-${phase}-${timestamp}`;

    const detailTrend = extractTrendMetrics(data.metrics?.champion_stats_detail_response_time);
    const detailRate = extractRateMetrics(data.metrics?.champion_stats_detail_success_rate);
    const positionsTrend = extractTrendMetrics(data.metrics?.champion_stats_positions_response_time);
    const positionsRate = extractRateMetrics(data.metrics?.champion_stats_positions_success_rate);

    const httpReqs = data.metrics?.http_reqs?.values?.count || 0;
    const httpRps = round(data.metrics?.http_reqs?.values?.rate);
    const customErrors = data.metrics?.custom_errors?.values?.count || 0;

    const jsonResult = {
        meta: {
            phase: phase,
            timestamp: new Date().toISOString(),
            platform: platformId,
            patch: patch,
            tier: tier,
            rate: Number(__ENV.RATE || 50),
            duration: __ENV.DURATION || '1m',
        },
        detail_api: detailTrend ? { ...detailTrend, ...detailRate } : null,
        positions_api: positionsTrend ? { ...positionsTrend, ...positionsRate } : null,
        totals: {
            http_requests: httpReqs,
            http_rps: httpRps,
            custom_errors: customErrors,
        },
    };

    const separator = '========================================';
    const customText = `\n${separator}\n` +
        `  Champion Stats 성능 테스트 결과\n` +
        `  Phase: ${phase}\n` +
        `${separator}\n\n` +
        formatApiSection('Detail API', detailTrend, detailRate) + '\n' +
        formatApiSection('Positions API', positionsTrend, positionsRate) +
        `${separator}\n`;

    return {
        [`../results/${baseName}.json`]: JSON.stringify(jsonResult, null, 2),
        [`../results/${baseName}.txt`]: customText,
        stdout: textSummary(data, { indent: '  ', enableColors: true }) + customText,
    };
}
