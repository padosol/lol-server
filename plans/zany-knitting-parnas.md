# 챔피언 매치업 통계 연동

## Context
챔피언 상세 통계 API에서 매치업(상대 챔피언별 승률/대면 비율) 데이터가 `null`로 반환되고 있음.
인프라 계층(ClickHouse 어댑터, 쿼리, ReadModel)은 이미 완성 상태이지만, 서비스 계층에서 포트를 호출하지 않고 `null`을 하드코딩하고 있음.

## 수정 사항

### 1. `ChampionStatsService.buildPositionStats()` — matchup 조회 연동

**파일**: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/championstats/application/ChampionStatsService.java`

`buildPositionStats()` 메서드에서:
- `championStatsQueryPort.getChampionMatchups(championId, patch, platformId, tier, position)` 호출 추가
- `ChampionPositionStatsReadModel` 생성자의 `null` → 실제 matchup 결과로 교체

```java
// before
return new ChampionPositionStatsReadModel(
    position, winRate.totalWinRate(), winRate.totalGames(),
    null, runeBuilds, spellStats, skillBuilds, ...);

// after
List<ChampionMatchupReadModel> matchups =
    championStatsQueryPort.getChampionMatchups(championId, patch, platformId, tier, position);

return new ChampionPositionStatsReadModel(
    position, winRate.totalWinRate(), winRate.totalGames(),
    matchups, runeBuilds, spellStats, skillBuilds, ...);
```

### 2. `ChampionStatsServiceTest` — matchup 검증 수정

**파일**: `module/core/lol-server-domain/src/test/java/com/example/lolserver/domain/championstats/application/ChampionStatsServiceTest.java`

- `getChampionMatchups` 포트 호출에 대한 mock 설정 추가
- `assertThat(middleStats.matchups()).isNull()` → 실제 matchup 데이터 검증으로 변경

## 이미 구현 완료된 항목 (수정 불필요)

| 계층 | 파일 | 상태 |
|------|------|------|
| ReadModel | `ChampionMatchupReadModel` | 완성 (opponentChampionId, games, winRate, pickRate) |
| Port | `ChampionStatsQueryPort.getChampionMatchups()` | 인터페이스 선언 완료 |
| Adapter | `ChampionStatsClickHouseAdapter.getChampionMatchups()` | SQL 쿼리 구현 완료 (`champion_matchup_stats_agg` 테이블 조회) |
| Adapter Test | `ChampionStatsClickHouseAdapterTest` | 테스트 완료 |
| Controller | `ChampionStatsController` | 서비스 결과를 그대로 반환 (변경 불필요) |
| RestDocs | `ChampionStatsControllerTest` | matchup 필드 문서화 완료 |

## 검증
```bash
./gradlew :module:core:lol-server-domain:test --tests "*ChampionStatsServiceTest"
./gradlew :module:infra:api:test
```
