# 챔피언 통계 각 항목 LIMIT 2 적용

## Context

챔피언 상세 통계 조회(`getChampionStats`) 시 매치업, 룬, 스펠, 스킬빌드, 시작아이템, 코어빌드, 코어순서별 아이템 등
모든 하위 항목이 LIMIT 없이 전체 결과를 반환하고 있음.
각 항목을 **최대 2개**까지만 조회하도록 SQL에 `LIMIT 2`를 추가한다.

## 수정 파일

### `ChampionStatsClickHouseAdapter.java`
- 경로: `module/infra/persistence/clickhouse/src/main/java/.../adapter/ChampionStatsClickHouseAdapter.java`
- 7개 메서드의 SQL 쿼리 `ORDER BY ... DESC` 뒤에 `LIMIT 2` 추가:
  1. `getChampionMatchups()` — `ORDER BY ms.games DESC LIMIT 2`
  2. `getChampionRuneBuilds()` — `ORDER BY rs.games DESC LIMIT 2`
  3. `getChampionSpellStats()` — `ORDER BY ss.games DESC LIMIT 2`
  4. `getChampionSkillBuilds()` — `ORDER BY sk.games DESC LIMIT 2`
  5. `getChampionStartItemBuilds()` — `ORDER BY its.games DESC LIMIT 2`
  6. `getChampionItemBuilds()` — `ORDER BY bs.games DESC LIMIT 2`
  7. `getChampionItemStats()` — `ORDER BY its.games DESC LIMIT 2`

### 테스트 파일 (영향 없음)
- 기존 테스트는 Mock 기반이므로 SQL LIMIT 추가에 영향받지 않음
- 테스트 데이터가 이미 1개씩이므로 수정 불필요

## 변경 범위 외
- `getChampionWinRate()` — 단일 결과 반환, LIMIT 불필요
- `getChampionStatsByPosition()` — 포지션별 전체 챔피언 목록, LIMIT 불필요
- Port/Service/Controller 계층 — 변경 없음 (SQL 레벨에서만 제한)

## 검증
- `./gradlew :module:infra:persistence:clickhouse:test --tests "...ChampionStatsClickHouseAdapterTest"` 통과 확인
- `./gradlew :module:core:lol-server-domain:test --tests "...ChampionStatsServiceTest"` 통과 확인
- `./gradlew :module:infra:api:test --tests "...ChampionStatsControllerTest"` 통과 확인
