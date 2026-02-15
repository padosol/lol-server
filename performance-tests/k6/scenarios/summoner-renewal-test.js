import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Trend, Rate } from 'k6/metrics';
import { BASE_URL, ENDPOINTS, HEADERS, DEFAULT_REGION } from '../lib/config.js';
import { randomItem } from '../lib/helpers.js';

/**
 * 소환사 검색 → 전적 갱신 시나리오 부하테스트
 *
 * 목적: 유저가 소환사를 검색하고 전적 갱신 버튼을 누르는 실제 사용자 흐름 시뮬레이션
 * 흐름:
 *   1. GET /api/v1/{region}/summoners/{puuid} - 소환사 검색
 *   2. Think time (1~3초)
 *   3. GET /api/summoners/renewal/{platform}/{puuid} - 전적 갱신
 */

// 테스트 데이터 로드
const puuids = new SharedArray('challenger_puuids', function () {
    const data = JSON.parse(open('../../data/challenger_summoner.json'));
    return data.entries.map(entry => entry.puuid);
});

// 커스텀 메트릭
const summonerSearchDuration = new Trend('summoner_search_duration', true);
const renewalDuration = new Trend('renewal_duration', true);
const summonerSearchSuccess = new Rate('summoner_search_success');
const renewalSuccess = new Rate('renewal_success');

// k6 옵션 - 낮은 TPS로 안정성 확인
export const options = {
    scenarios: {
        summoner_renewal: {
            executor: 'constant-arrival-rate',
            rate: 1,
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 2,
            maxVUs: 5,
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<5000'],
        'http_req_failed': ['rate<0.3'],
        'summoner_search_duration': ['p(95)<5000'],
        'renewal_duration': ['p(95)<5000'],
        'summoner_search_success': ['rate>0.7'],
        'renewal_success': ['rate>0.7'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
    tags: {
        testType: 'smoke',
        scenario: 'summoner-renewal-test',
    },
};

const requestOptions = {
    headers: HEADERS,
    timeout: '30s',
};

export default function () {
    const puuid = randomItem(puuids);
    const region = DEFAULT_REGION;

    // Step 1: 소환사 검색
    const searchUrl = `${BASE_URL}${ENDPOINTS.summoner.getByPuuid(region, puuid)}`;
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
            console.error(`Failed to parse search response for puuid: ${puuid}`);
        }
    } else {
        console.error(`Search failed - Status: ${searchResponse.status}, PUUID: ${puuid}`);
    }

    if (!searchSuccess) {
        return;
    }

    // Step 2: Think time (1~3초)
    sleep(1 + Math.random() * 2);

    // Step 3: 전적 갱신
    const renewalUrl = `${BASE_URL}${ENDPOINTS.summoner.renewal(region, puuid)}`;
    console.log(renewalUrl)
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
            console.error(`Failed to parse renewal response for puuid: ${puuid}`);
        }
    } else {
        console.error(`Renewal failed - Status: ${renewalResponse.status}, PUUID: ${puuid}`);
    }
}
