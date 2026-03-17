# 챔피언 매치업 유리/불리 상대 분리 (limit 3)

## Context

현재 챔피언 상세 통계 API에서 매치업 데이터는 단순히 게임 수 기준 상위 2개를 반환한다.
이를 **유리한 상대 (strongMatchups, 승률 높은 순 3개)** 와 **불리한 상대 (weakMatchups, 승률 낮은 순 3개)** 로 분리하여 사용자에게 더 유의미한 정보를 제공한다.

## 변경 사항 요약

| 계층 | 파일 | 변경 내용 |
|------|------|----------|
| ReadModel | `ChampionPositionStatsReadModel.java` | `matchups` → `strongMatchups` + `weakMatchups` |
| Port | `ChampionStatsQueryPort.java` | `getChampionMatchups` → `getStrongMatchups` + `getWeakMatchups` |
| Adapter | `ChampionStatsClickHouseAdapter.java` | SQL 2개 (ORDER BY win_rate DESC/ASC, HAVING games >= 50, LIMIT 3) |
| Service | `ChampionStatsService.java` | 포트 호출 2회로 변경 |
| Test | `ChampionStatsServiceTest.java` | mock/assertion 업데이트 |
| Test | `ChampionStatsClickHouseAdapterTest.java` | 테스트 2개로 분리 |
| Test | `ChampionStatsControllerTest.java` (RestDocs) | 응답 필드 문서화 업데이트 |

## 구현 순서

### 1. `ChampionPositionStatsReadModel` 수정

**파일:** `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/championstats/application/model/ChampionPositionStatsReadModel.java`

```java
public record ChampionPositionStatsReadModel(
    String teamPosition,
    double winRate,
    long totalGames,
    List<ChampionMatchupReadModel> strongMatchups,   // 유리한 상대 (승률 높은 순)
    List<ChampionMatchupReadModel> weakMatchups,      // 불리한 상대 (승률 낮은 순)
    List<ChampionRuneBuildReadModel> runeBuilds,
    ...나머지 동일
) {}
```

### 2. `ChampionStatsQueryPort` 수정

**파일:** `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/championstats/application/port/out/ChampionStatsQueryPort.java`

- `getChampionMatchups` 제거
- `getStrongMatchups` 추가 (동일 시그니처)
- `getWeakMatchups` 추가 (동일 시그니처)

### 3. `ChampionStatsClickHouseAdapter` 수정

**파일:** `module/infra/persistence/clickhouse/src/main/java/com/example/lolserver/repository/championstats/adapter/ChampionStatsClickHouseAdapter.java`

- `getChampionMatchups` 제거
- private helper `queryMatchups(... , String orderDirection)` 추출 (SQL 중복 방지)
- `HAVING games >= 50` 추가 (통계적 유의성 확보, `ChampionTierCalculator.MIN_GAMES`와 동일 기준)
- `LIMIT 3`으로 변경 (기존 LIMIT 2 → 3)

```sql
WITH
    matchup_stats AS (
        SELECT opponent_champion_id, sum(games) AS games, sum(wins) AS wins
        FROM champion_matchup_stats_agg
        WHERE patch_version = ? AND platform_id = ? AND tier = ?
              AND champion_id = ? AND team_position = ?
        GROUP BY opponent_champion_id
        HAVING games >= 50
    ),
    total AS (SELECT sum(games) AS total_games FROM matchup_stats)
SELECT ms.opponent_champion_id, ms.games,
       ms.wins / ms.games       AS win_rate,
       ms.games / t.total_games AS pick_rate
FROM matchup_stats AS ms CROSS JOIN total AS t
ORDER BY win_rate DESC  -- strong: DESC / weak: ASC
LIMIT 3
```

### 4. `ChampionStatsService` 수정

**파일:** `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/championstats/application/ChampionStatsService.java`

`buildPositionStats()`에서:
```java
List<ChampionMatchupReadModel> strongMatchups =
    championStatsQueryPort.getStrongMatchups(championId, patch, platformId, tier, position);
List<ChampionMatchupReadModel> weakMatchups =
    championStatsQueryPort.getWeakMatchups(championId, patch, platformId, tier, position);
```

### 5. 테스트 업데이트

**5a. `ChampionStatsServiceTest`** — mock stub을 `getStrongMatchups`/`getWeakMatchups`로 변경, assertion에서 `strongMatchups()`/`weakMatchups()` 검증

**5b. `ChampionStatsClickHouseAdapterTest`** — `getChampionMatchups` 테스트를 `getStrongMatchups`/`getWeakMatchups` 2개로 분리

**5c. `ChampionStatsControllerTest` (RestDocs)** — `ChampionPositionStatsReadModel` 생성자 호출 업데이트, 응답 필드 문서에서 `matchups[]` → `strongMatchups[]` + `weakMatchups[]`

## 검증

```bash
./gradlew test
```

- `ChampionStatsServiceTest` — 유리/불리 매치업 분리 반환 검증
- `ChampionStatsClickHouseAdapterTest` — 어댑터 메서드 호출 검증
- `ChampionStatsControllerTest` — RestDocs 생성 및 API 응답 구조 검증
