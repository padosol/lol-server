import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

/**
 * 커스텀 메트릭 정의
 */
export const customMetrics = {
    // 에러 카운터
    errorCount: new Counter('custom_errors'),

    // API별 응답 시간
    summonerResponseTime: new Trend('summoner_response_time', true),
    autocompleteResponseTime: new Trend('autocomplete_response_time', true),
    matchResponseTime: new Trend('match_response_time', true),
    leagueResponseTime: new Trend('league_response_time', true),
    championRotationResponseTime: new Trend('champion_rotation_response_time', true),
    rankChampionsResponseTime: new Trend('rank_champions_response_time', true),

    // API별 성공률
    summonerSuccessRate: new Rate('summoner_success_rate'),
    autocompleteSuccessRate: new Rate('autocomplete_success_rate'),
    matchSuccessRate: new Rate('match_success_rate'),
    leagueSuccessRate: new Rate('league_success_rate'),
    championRotationSuccessRate: new Rate('champion_rotation_success_rate'),
    rankChampionsSuccessRate: new Rate('rank_champions_success_rate'),
};

/**
 * API 응답 검증 함수
 * @param {Object} response - k6 HTTP 응답 객체
 * @param {string} apiName - API 이름 (로깅용)
 * @param {number} expectedStatus - 예상 HTTP 상태 코드
 * @returns {boolean} 검증 성공 여부
 */
export function validateResponse(response, apiName, expectedStatus = 200) {
    const checks = check(response, {
        [`${apiName}: status is ${expectedStatus}`]: (r) => r.status === expectedStatus,
        [`${apiName}: response time < 5s`]: (r) => r.timings.duration < 5000,
        [`${apiName}: response body exists`]: (r) => r.body && r.body.length > 0,
    });

    if (!checks) {
        customMetrics.errorCount.add(1);
        console.error(`[${apiName}] Validation failed - Status: ${response.status}, Duration: ${response.timings.duration}ms`);
    }

    return checks;
}

/**
 * API 응답의 success 필드 검증
 * @param {Object} response - k6 HTTP 응답 객체
 * @param {string} apiName - API 이름
 * @returns {boolean} 검증 성공 여부
 */
export function validateApiSuccess(response, apiName) {
    try {
        const body = JSON.parse(response.body);
        const isSuccess = body.success === true;

        if (!isSuccess) {
            console.error(`[${apiName}] API returned success=false: ${JSON.stringify(body)}`);
        }

        return check(response, {
            [`${apiName}: API success`]: () => isSuccess,
        });
    } catch (e) {
        console.error(`[${apiName}] Failed to parse response: ${e.message}`);
        return false;
    }
}

/**
 * 랜덤 배열 요소 선택
 * @param {Array} array - 배열
 * @returns {*} 랜덤 요소
 */
export function randomItem(array) {
    return array[Math.floor(Math.random() * array.length)];
}

/**
 * 랜덤 정수 생성
 * @param {number} min - 최소값
 * @param {number} max - 최대값
 * @returns {number} 랜덤 정수
 */
export function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

/**
 * 검색어 생성 (자동완성용)
 * @param {string} fullName - 전체 이름
 * @param {number} length - 검색어 길이
 * @returns {string} 부분 검색어
 */
export function generateSearchQuery(fullName, length = 2) {
    return fullName.substring(0, Math.min(length, fullName.length));
}

/**
 * 슬립 함수 (think time 시뮬레이션)
 * @param {number} minMs - 최소 대기 시간 (밀리초)
 * @param {number} maxMs - 최대 대기 시간 (밀리초)
 */
export function randomSleep(minMs, maxMs) {
    const sleepTime = randomInt(minMs, maxMs) / 1000;
    return sleepTime;
}

/**
 * 응답에서 puuid 추출
 * @param {Object} response - API 응답
 * @returns {string|null} puuid
 */
export function extractPuuid(response) {
    try {
        const body = JSON.parse(response.body);
        return body.data?.puuid || null;
    } catch (e) {
        return null;
    }
}

/**
 * 응답에서 매치 ID 목록 추출
 * @param {Object} response - API 응답
 * @returns {Array} 매치 ID 배열
 */
export function extractMatchIds(response) {
    try {
        const body = JSON.parse(response.body);
        return body.data?.content || [];
    } catch (e) {
        return [];
    }
}

/**
 * HTTP 요청 옵션 생성
 * @param {Object} headers - 추가 헤더
 * @param {string} timeout - 타임아웃
 * @returns {Object} 요청 옵션
 */
export function createRequestOptions(headers = {}, timeout = '30s') {
    return {
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...headers,
        },
        timeout: timeout,
    };
}
