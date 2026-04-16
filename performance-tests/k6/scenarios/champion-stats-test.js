import http from 'k6/http';
import { check } from 'k6';

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

// stages, thresholds, scenarios
export const options = {
    scenarios: {
        champion_stats_load: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.RATE || 300),
            timeUnit: '1s',
            duration: __ENV.DURATION || '1m',
            preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 50),
            maxVUs: Number(__ENV.MAX_VUS || 500),
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'], // 에러율 1% 미만
        http_req_duration: ['p(95) < 300', 'p(99) < 500'],
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

