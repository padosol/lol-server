import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { Trend, Rate } from 'k6/metrics';
import exec from 'k6/execution';
import { BASE_URL, ENDPOINTS, HEADERS, DEFAULT_PLATFORM_ID } from '../lib/config.js';

/**
 * 순차적 유저 검색 → 갱신 부하테스트
 *
 * 목적: 200초 동안 1초마다 한 명의 유저를 순차적으로 검색 후 갱신
 * 핵심: randomItem 대신 iterationInTest를 사용하여 순차적 접근
 * 흐름:
 *   1. GET /api/v1/{platformId}/summoners/{puuid} - 소환사 검색
 *   2. 검색 성공 시 GET /api/v1/{platformId}/summoners/{puuid}/renewal - 전적 갱신
 */

const puuids = new SharedArray('master_puuids', function () {
    const data = JSON.parse(open('../../data/grand_master_summoner.json'));
    return data.entries.map(entry => entry.puuid);
});

// 커스텀 메트릭
const summonerSearchDuration = new Trend('summoner_search_duration', true);
const summonerSearchSuccess = new Rate('summoner_search_success');
const renewalDuration = new Trend('renewal_duration', true);
const renewalSuccess = new Rate('renewal_success');

export const options = {
    scenarios: {
        sequential_renewal: {
            executor: 'constant-arrival-rate',
            rate: 1,
            timeUnit: '1s',
            duration: '200s',
            preAllocatedVUs: 1,
            maxVUs: 3,
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<5000'],
        'http_req_failed': ['rate<0.3'],
        'summoner_search_duration': ['p(95)<5000'],
        'summoner_search_success': ['rate>0.7'],
        'renewal_duration': ['p(95)<5000'],
        'renewal_success': ['rate>0.7'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
    tags: {
        testType: 'load',
        scenario: 'sequential-renewal-test',
    },
};

const requestOptions = {
    headers: HEADERS,
    timeout: '30s',
};

export default function () {
    const START_OFFSET = 200;
    const iterationIndex = exec.scenario.iterationInTest;
    const puuid = puuids[(START_OFFSET + iterationIndex) % puuids.length];
    const platformId = DEFAULT_PLATFORM_ID;

    // Step 1: 소환사 검색
    const searchUrl = `${BASE_URL}${ENDPOINTS.summoner.getByPuuid(platformId, puuid)}`;
    const searchResponse = http.get(searchUrl, requestOptions);

    const searchOk = check(searchResponse, {
        'search: status is 200': (r) => r.status === 200,
        'search: response time < 5s': (r) => r.timings.duration < 5000,
        'search: response body exists': (r) => r.body && r.body.length > 0,
    });

    summonerSearchDuration.add(searchResponse.timings.duration);
    summonerSearchSuccess.add(searchOk);

    // 검색 성공 시에만 갱신 호출 (실제 사용자 흐름: 검색 실패 시 갱신 버튼 없음)
    let searchSuccess = false;
    if (searchResponse.status === 200) {
        try {
            const body = JSON.parse(searchResponse.body);
            searchSuccess = body.result === 'SUCCESS';
            check(searchResponse, {
                'search: API success': () => searchSuccess,
            });
        } catch (e) {
            console.error(`[#${iterationIndex}] Failed to parse search response for puuid: ${puuid}`);
        }
    } else {
        console.error(`[#${iterationIndex}] Search failed - Status: ${searchResponse.status}, PUUID: ${puuid}`);
    }

    if (!searchSuccess) {
        return;
    }

    // Step 2: 전적 갱신
    console.log(`[#${iterationIndex}] Search OK → Renewing puuid: ${puuid}`);

    const renewalUrl = `${BASE_URL}${ENDPOINTS.summoner.renewal(platformId, puuid)}`;
    const renewalResponse = http.get(renewalUrl, requestOptions);

    const renewalOk = check(renewalResponse, {
        'renewal: status is 200': (r) => r.status === 200,
        'renewal: response time < 5s': (r) => r.timings.duration < 5000,
        'renewal: response body exists': (r) => r.body && r.body.length > 0,
    });

    renewalDuration.add(renewalResponse.timings.duration);
    renewalSuccess.add(renewalOk);

    if (renewalResponse.status === 200) {
        try {
            const body = JSON.parse(renewalResponse.body);
            check(renewalResponse, {
                'renewal: API success': () => body.result === 'SUCCESS',
            });
        } catch (e) {
            console.error(`[#${iterationIndex}] Failed to parse renewal response for puuid: ${puuid}`);
        }
    } else {
        console.error(`[#${iterationIndex}] Renewal failed - Status: ${renewalResponse.status}, PUUID: ${puuid}`);
    }
}
