import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';

import { BASE_URL, DEFAULT_PLATFORM_ID, ENDPOINTS, HEADERS } from '../lib/config.js';
import { customMetrics, randomItem } from '../lib/helpers.js';

/**
 * PUUID 기반 매치 목록 조회 부하 테스트
 *
 * 대상 API: GET /api/v1/{platformId}/summoners/{puuid}/matches
 * 목적: 매치 조회 시 응답시간과 DB 커넥션 풀 대기 여부를 함께 관찰
 */

const puuids = new SharedArray('match_puuids', function () {
    const data = JSON.parse(open('../../data/master_summoners.json'));
    return data.entries.map((entry) => entry.puuid).filter(Boolean);
});

const platformId = __ENV.PLATFORM_ID || DEFAULT_PLATFORM_ID;
const season = __ENV.SEASON;
const queueId = __ENV.QUEUE_ID;
const pageNo = Number(__ENV.PAGE_NO || 1);

export const options = {
    scenarios: {
        puuid_match_lookup: {
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
        match_response_time: ['p(95)<300', 'p(99)<500'],
        // http_req_duration: [{
        //     threshold: 'p(95) > 2000',
        //     abortOnFail: true
        // }]
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
    tags: {
        testType: 'load',
        scenario: 'puuid-match-test',
        endpoint: 'summoner-matches',
    },
};

const requestOptions = {
    headers: HEADERS,
    timeout: '30s',
};

export default function () {
    const puuid = randomItem(puuids);
    const endpoint = ENDPOINTS.match.getMatchesByPuuid(platformId, puuid, season, queueId, pageNo);
    const url = `${BASE_URL}${endpoint}`;
    const response = http.get(url, requestOptions);

    customMetrics.matchResponseTime.add(response.timings.duration);

    const basicChecks = check(response, {
        'match lookup: status is 200': (r) => r.status === 200,
        'match lookup: response time < 5s': (r) => r.timings.duration < 5000,
        'match lookup: response body exists': (r) => r.body && r.body.length > 0,
    });

    let apiSuccess = false;
    let contentExists = false;

    if (response.status === 200) {
        try {
            const body = JSON.parse(response.body);
            apiSuccess = body.result === 'SUCCESS';
            contentExists = Array.isArray(body.data?.content);

            check(response, {
                'match lookup: API success': () => apiSuccess,
                'match lookup: content is array': () => contentExists,
            });
        } catch (error) {
            console.error(`[match lookup] Failed to parse response for puuid=${puuid}: ${error.message}`);
        }
    } else {
        console.error(
            `[match lookup] Request failed status=${response.status} duration=${response.timings.duration}ms puuid=${puuid} url=${url}`
        );
    }

    const success = basicChecks && apiSuccess && contentExists;
    customMetrics.matchSuccessRate.add(success);

    if (!success) {
        customMetrics.errorCount.add(1);
    }
}
