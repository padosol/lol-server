import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { Trend, Rate } from 'k6/metrics';
import { BASE_URL, ENDPOINTS, HEADERS, DEFAULT_REGION } from '../lib/config.js';

/**
 * PUUID 기반 소환사 검색 성능 테스트
 *
 * 목적: master_summoners.json의 puuid를 사용하여 소환사 검색 API 성능 측정
 * 대상 API: GET /api/v1/{region}/summoners/{puuid}
 */

// 테스트 데이터 로드 (master_summoners.json에서 puuid 추출)
const masterPuuids = new SharedArray('master_puuids', function () {
    const data = JSON.parse(open('../../data/master_summoners.json'));
    return data.entries.map(entry => entry.puuid);
});

// k6 옵션 - 50 VU가 초당 1번씩 30초 동안 호출
export const options = {
    scenarios: {
        puuid_search: {
            executor: 'constant-arrival-rate',
            rate: 30,              // 비율
            timeUnit: '1s',        // 시간 == > 비율 / 시간
            duration: '30s',       // 지속 시간
            preAllocatedVUs: 30,   // 시작시
            maxVUs: 30,            // 가상 유저수
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<3000'],  // 95% 요청이 3초 이내
        'http_req_failed': ['rate<0.1'],      // 실패율 10% 미만
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
    tags: {
        testType: 'load',
        scenario: 'puuid-search-test',
    },
};

// 요청 옵션
const requestOptions = {
    headers: HEADERS,
    timeout: '30s',
};

export default function () {
    // 랜덤 puuid 선택
    const puuid = masterPuuids[Math.floor(Math.random() * masterPuuids.length)];
    const region = DEFAULT_REGION;

    // API 호출: GET /api/v1/{region}/summoners/{puuid}
    const url = `${BASE_URL}${ENDPOINTS.summoner.getByPuuid(region, puuid)}`;
    const response = http.get(url, requestOptions);

    // 응답 검증
    const isSuccess = check(response, {
        'status is 200': (r) => r.status === 200,
        'response time < 5s': (r) => r.timings.duration < 5000,
        'response body exists': (r) => r.body && r.body.length > 0,
    });

    // API 응답 검증 (success 필드)
    if (response.status === 200) {
        try {
            const body = JSON.parse(response.body);
            check(response, {
                'API success': () => body.result === 'SUCCESS',
            });

            if (body.result !== 'SUCCESS') {
                console.warn(`API returned result=${body.result} for puuid: ${puuid}`);
            }
        } catch (e) {
            console.error(`Failed to parse response for puuid: ${puuid}`);
        }
    } else {
        console.error(`Request failed - Status: ${response.status}, PUUID: ${puuid}`);
    }
}
