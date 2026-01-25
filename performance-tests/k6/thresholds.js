/**
 * k6 성능 테스트 임계값 정의
 *
 * 성능 목표:
 * - p95 < 500ms (전체)
 * - p99 < 1000ms (전체)
 * - 에러율 < 1%
 */

export const thresholds = {
    // 전체 HTTP 요청 메트릭
    http_req_duration: [
        'p(95)<500',    // p95 응답시간 500ms 미만
        'p(99)<1000',   // p99 응답시간 1000ms 미만
    ],
    http_req_failed: ['rate<0.01'],  // 에러율 1% 미만

    // Summoner API - DB + 외부 API 호출
    summoner_response_time: [
        'p(95)<300',    // p95 300ms 미만
        'avg<200',      // 평균 200ms 미만
    ],
    summoner_success_rate: ['rate>0.999'],  // 성공률 99.9% 이상

    // Autocomplete API - DB LIKE 쿼리
    autocomplete_response_time: [
        'p(95)<150',    // p95 150ms 미만 (빠른 응답 필요)
        'avg<100',      // 평균 100ms 미만
    ],
    autocomplete_success_rate: ['rate>0.999'],

    // Match API - 페이징, 복잡한 조인
    match_response_time: [
        'p(95)<500',    // p95 500ms 미만
        'avg<300',      // 평균 300ms 미만
    ],
    match_success_rate: ['rate>0.995'],  // 성공률 99.5% 이상

    // League API - 단일 조회
    league_response_time: [
        'p(95)<300',    // p95 300ms 미만
        'avg<150',      // 평균 150ms 미만
    ],
    league_success_rate: ['rate>0.995'],

    // Champion Rotation API - Redis 캐시
    champion_rotation_response_time: [
        'p(95)<100',    // p95 100ms 미만 (캐시 응답)
        'avg<50',       // 평균 50ms 미만
    ],
    champion_rotation_success_rate: ['rate>0.9999'],  // 성공률 99.99% 이상

    // Rank Champions API - 통계 집계
    rank_champions_response_time: [
        'p(95)<400',    // p95 400ms 미만
        'avg<250',      // 평균 250ms 미만
    ],
    rank_champions_success_rate: ['rate>0.995'],

    // 커스텀 에러 카운터
    custom_errors: ['count<10'],  // 커스텀 에러 10회 미만
};

/**
 * Smoke Test용 완화된 임계값
 */
export const smokeThresholds = {
    http_req_duration: ['p(95)<1000'],  // 더 관대한 응답시간
    http_req_failed: ['rate<0.05'],     // 에러율 5% 미만 (초기 테스트)
};

/**
 * Load Test용 임계값
 */
export const loadThresholds = {
    ...thresholds,
    // 부하 테스트에서는 약간 더 관대하게
    http_req_duration: [
        'p(95)<600',
        'p(99)<1200',
    ],
};

/**
 * Stress Test용 임계값
 */
export const stressThresholds = {
    http_req_duration: [
        'p(95)<2000',   // 스트레스 상황에서는 더 관대
        'p(99)<5000',
    ],
    http_req_failed: ['rate<0.05'],  // 에러율 5% 미만
};

/**
 * Champion Rotation 전용 테스트 임계값
 *
 * 특성: Redis 캐시 기반, 빠른 응답 기대
 * 목표: p95 < 100ms, 에러율 < 0.01%
 */
export const championRotationThresholds = {
    // 전체 HTTP 메트릭
    http_req_failed: ['rate<0.0001'],  // 에러율 0.01% 미만

    // 캐시 히트 시나리오 (단일 리전 집중)
    cache_hit_response_time: [
        'p(95)<100',    // p95 100ms 미만
        'avg<50',       // 평균 50ms 미만
    ],
    cache_hit_success_rate: ['rate>0.9999'],  // 성공률 99.99% 이상

    // 멀티 리전 시나리오 (여러 리전 동시 요청)
    multi_region_response_time: [
        'p(95)<150',    // p95 150ms 미만 (리전별 차이 고려)
        'avg<80',       // 평균 80ms 미만
    ],
    multi_region_success_rate: ['rate>0.9999'],

    // 스파이크 시나리오 (급격한 부하 변화)
    spike_response_time: [
        'p(95)<300',    // p95 300ms 미만 (스파이크 상황 고려)
        'avg<150',      // 평균 150ms 미만
    ],
    spike_success_rate: ['rate>0.999'],  // 스파이크 시 99.9% 허용

    // 공통 Champion Rotation 메트릭
    champion_rotation_response_time: [
        'p(95)<100',    // p95 100ms 미만
        'avg<50',       // 평균 50ms 미만
    ],
    champion_rotation_success_rate: ['rate>0.9999'],
};
