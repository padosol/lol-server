# k6 부하테스트 엔드포인트 수정 계획

## Context

`region → platformId` 리네이밍 및 API 경로 구조 변경 이후, k6 부하테스트의 엔드포인트 정의(`config.js`)가 실제 컨트롤러 매핑과 불일치. 테스트 실행 시 404 응답이 발생할 수 있으므로 현재 API에 맞게 동기화 필요.

## 불일치 엔드포인트 비교

| API | k6 config.js (현재) | 컨트롤러 (실제) |
|-----|-------------------|----------------|
| summoner.autocomplete | `/api/v1/summoners/autocomplete?q=&region=` | `/api/v1/{platformId}/summoners/autocomplete?q=` |
| summoner.renewal | `/api/summoners/renewal/{platform}/{puuid}` | `/api/v1/{platformId}/summoners/{puuid}/renewal` |
| match.getMatchIds | `/api/v1/matches/matchIds?puuid=&page=&size=` | `/api/v1/{platformId}/matches/matchIds?puuid=&page=&size=` |
| match.getMatches | `/api/v1/matches?puuid=&page=&size=` | `/api/v1/{platformId}/matches?puuid=&page=&size=` |
| rank.get | `/api/v1/rank?tier=&division=&page=` | `/api/v1/{platformId}/rank?...` |

변경 불필요 (이미 일치):
- `summoner.getByGameName`, `summoner.getByPuuid`, `summoner.renewalStatus`
- `match.getById`, `match.timeline`
- `champion.rotation`, `league.getByPuuid`, `rank.champions`

## 수정 사항

### 1. `config.js` 엔드포인트 정의 수정

**파일:** `performance-tests/k6/lib/config.js`

파라미터명 `region` → `platformId` 통일 + 경로 수정:

```js
export const ENDPOINTS = {
    summoner: {
        getByGameName: (platformId, gameName) => `/api/v1/summoners/${platformId}/${encodeURIComponent(gameName)}`,
        getByPuuid: (platformId, puuid) => `/api/v1/${platformId}/summoners/${puuid}`,
        autocomplete: (platformId, q) => `/api/v1/${platformId}/summoners/autocomplete?q=${encodeURIComponent(q)}`,
        renewal: (platformId, puuid) => `/api/v1/${platformId}/summoners/${puuid}/renewal`,
        renewalStatus: (puuid) => `/api/v1/summoners/${puuid}/renewal-status`,
    },
    match: {
        getById: (matchId) => `/api/v1/matches/${matchId}`,
        getMatchIds: (platformId, puuid, page = 0, size = 10) => `/api/v1/${platformId}/matches/matchIds?puuid=${puuid}&page=${page}&size=${size}`,
        getMatches: (platformId, puuid, page = 0, size = 10) => `/api/v1/${platformId}/matches?puuid=${puuid}&page=${page}&size=${size}`,
        timeline: (matchId) => `/api/v1/match/timeline/${matchId}`,
    },
    champion: {
        rotation: (platformId) => `/api/v1/${platformId}/champion/rotation`,
    },
    league: {
        getByPuuid: (puuid) => `/api/v1/leagues/by-puuid/${puuid}`,
    },
    rank: {
        champions: (puuid, queueId = 420) => `/api/v1/rank/champions?puuid=${puuid}&queueId=${queueId}`,
        get: (platformId, tier, division, page = 0) => `/api/v1/${platformId}/rank?tier=${tier}&division=${division}&page=${page}`,
    },
};
```

### 2. `puuid-search-test.js` 변수명 통일

**파일:** `performance-tests/k6/scenarios/puuid-search-test.js`

- `region` → `platformId` 변수명 변경 (기능 동작은 동일, 네이밍 통일)

### 3. `summoner-renewal-test.js` 변수명 통일

**파일:** `performance-tests/k6/scenarios/summoner-renewal-test.js`

- `region` → `platformId` 변수명 변경

### 4. `config.js` 상수명 변경

- `REGIONS` → `PLATFORM_IDS`
- `DEFAULT_REGION` → `DEFAULT_PLATFORM_ID`

## 수정 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `performance-tests/k6/lib/config.js` | 엔드포인트 경로 수정 + 파라미터명/상수명 `region` → `platformId` |
| `performance-tests/k6/scenarios/puuid-search-test.js` | `region` → `platformId` 변수명 + import명 변경 |
| `performance-tests/k6/scenarios/summoner-renewal-test.js` | `region` → `platformId` 변수명 + import명 변경 |

## 검증 방법

1. k6 스크립트 문법 오류 확인: `k6 inspect performance-tests/k6/scenarios/puuid-search-test.js`
2. 로컬 서버 실행 후 시나리오 실행하여 404 미발생 확인
