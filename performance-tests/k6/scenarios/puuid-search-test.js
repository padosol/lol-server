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

// k6 옵션 - 가장 가벼운 버전
export const options = {
    vus: 1,           // 단일 VU
    iterations: 10,   // 10회 반복
    thresholds: {
        'puuid_search_response_time': ['p(95)<3000'], // 95% 요청이 3초 이내
        'puuid_search_success_rate': ['rate>0.9'],    // 90% 이상 성공
        'http_req_failed': ['rate<0.1'],              // 실패율 10% 미만
    },
    tags: {
        testType: 'smoke',
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
                'API success': () => body.success === true,
            });

            if (!body.success) {
                console.warn(`API returned success=false for puuid: ${puuid}`);
            }
        } catch (e) {
            console.error(`Failed to parse response for puuid: ${puuid}`);
        }
    } else {
        console.error(`Request failed - Status: ${response.status}, PUUID: ${puuid}`);
    }
}
