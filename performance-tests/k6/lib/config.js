/**
 * k6 성능 테스트 공통 설정
 */

// 환경변수에서 BASE_URL 가져오기 (기본값: localhost:8100)
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8100';

// 공통 HTTP 헤더
export const HEADERS = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
};

// 타임아웃 설정 (밀리초)
export const TIMEOUTS = {
    request: '30s',
    connect: '10s',
};

// API 엔드포인트 정의
export const ENDPOINTS = {
    // Summoner API
    summoner: {
        getByGameName: (platformId, gameName) => `/api/v1/summoners/${platformId}/${encodeURIComponent(gameName)}`,
        getByPuuid: (platformId, puuid) => `/api/v1/${platformId}/summoners/${puuid}`,
        autocomplete: (platformId, q) => `/api/v1/${platformId}/summoners/autocomplete?q=${encodeURIComponent(q)}`,
        renewal: (platformId, puuid) => `/api/v1/${platformId}/summoners/${puuid}/renewal`,
        renewalStatus: (puuid) => `/api/v1/summoners/${puuid}/renewal-status`,
    },

    // Match API
    match: {
        getById: (matchId) => `/api/v1/matches/${matchId}`,
        getMatchIds: (platformId, puuid, page = 0, size = 10) => `/api/v1/${platformId}/matches/matchIds?puuid=${puuid}&page=${page}&size=${size}`,
        getMatches: (platformId, puuid, page = 0, size = 10) => `/api/v1/${platformId}/matches?puuid=${puuid}&page=${page}&size=${size}`,
        timeline: (matchId) => `/api/v1/match/timeline/${matchId}`,
    },

    // Champion API
    champion: {
        rotation: (platformId) => `/api/v1/${platformId}/champion/rotation`,
    },

    // League API
    league: {
        getByPuuid: (puuid) => `/api/v1/leagues/by-puuid/${puuid}`,
    },

    // Rank API
    rank: {
        champions: (puuid, queueId = 420) => `/api/v1/rank/champions?puuid=${puuid}&queueId=${queueId}`,
        get: (platformId, tier, division, page = 0) => `/api/v1/${platformId}/rank?tier=${tier}&division=${division}&page=${page}`,
    },
};

// 테스트 플랫폼
export const PLATFORM_IDS = ['kr', 'na1', 'euw1'];

// 기본 플랫폼
export const DEFAULT_PLATFORM_ID = 'kr';
