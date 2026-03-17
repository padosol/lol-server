# 부하테스트: 순차적 유저 갱신 (200초, 1 req/s)

## Context

200초 동안 1초마다 한 명의 유저를 순차적으로 **검색 후 갱신**하는 k6 부하테스트를 수정한다. 현재 `sequential-renewal-test.js`는 갱신만 바로 호출하지만, 실제 사용자 흐름처럼 소환사 검색 → 응답 확인 → 갱신 순서로 호출해야 한다.

## 수정 파일

**`performance-tests/k6/scenarios/sequential-renewal-test.js`** (이미 생성됨, 수정 필요)

## 변경 사항

### 커스텀 메트릭 추가
- `summonerSearchDuration` (Trend) — 소환사 검색 응답 시간
- `summonerSearchSuccess` (Rate) — 소환사 검색 성공률

### 테스트 흐름 변경 (iteration 당)
1. `exec.scenario.iterationInTest`로 현재 순번 획득
2. **Step 1: 소환사 검색** — `GET /api/v1/{platformId}/summoners/{puuid}` 호출
3. 검색 응답 검증 (status 200, API success)
4. 검색 실패 시 해당 iteration 종료 (return)
5. **Step 2: 전적 갱신** — `GET /api/v1/{platformId}/summoners/{puuid}/renewal` 호출
6. 갱신 응답 검증 및 메트릭 기록

### 참고 패턴
- `summoner-renewal-test.js`의 검색→갱신 2단계 흐름과 동일 (단, think time 없음, 순차적 유저 선택)
- `ENDPOINTS.summoner.getByPuuid(platformId, puuid)` — 검색 엔드포인트
- `ENDPOINTS.summoner.renewal(platformId, puuid)` — 갱신 엔드포인트

### Thresholds 추가
- `summoner_search_duration`: p(95) < 5000ms
- `summoner_search_success`: rate > 0.7

## 검증 방법

```bash
k6 run --env BASE_URL=http://localhost:8100 performance-tests/k6/scenarios/sequential-renewal-test.js
```

- 콘솔 로그에서 검색 → 갱신 순서로 호출되는지 확인
- 검색 실패 시 갱신이 스킵되는지 확인
