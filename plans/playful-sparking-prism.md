# ClickHouse 조회 테이블명 수정 계획

## Context

`docs/03_local_table.sql`에 ClickHouse 팩트 테이블 스키마가 정리되었다. 현재 어댑터 코드에서 2개 메서드가 문서에 존재하지 않는 테이블명(`champion_stats_local`, `champion_matchup_stats_local`)을 참조하고 있어, `docs/03_local_table.sql`에 정의된 실제 테이블명으로 수정한다.

## 변경 대상

### 파일: `ChampionStatsClickHouseAdapter.java`
경로: `module/infra/persistence/clickhouse/src/main/java/com/example/lolserver/repository/championstats/adapter/ChampionStatsClickHouseAdapter.java`

#### 1. `getChampionWinRate()` (line 46)
- 테이블: `champion_stats_local` → `match_participant_local`
- 컬럼: `patch` → `patch_version`
- 쿼리: `sum(games)` → `count(*)`, `sum(wins)` → `toInt64(sum(win))`
  - `match_participant_local`은 행 단위 데이터(win=0/1)이므로 집계 방식 변경

변경 전:
```sql
SELECT team_position,
       toInt64(sum(games)) AS total_games,
       toInt64(sum(wins)) AS total_wins,
       coalesce(round(sum(wins) / nullIf(sum(games), 0), 4), 0) AS total_win_rate
FROM champion_stats_local
WHERE champion_id = %d AND patch = %s AND platform_id = %s AND tier = %s
      AND team_position = %s
GROUP BY team_position
```

변경 후:
```sql
SELECT team_position,
       toInt64(count(*)) AS total_games,
       toInt64(sum(win)) AS total_wins,
       coalesce(round(sum(win) / nullIf(count(*), 0), 4), 0) AS total_win_rate
FROM match_participant_local
WHERE champion_id = %d AND patch_version = %s AND platform_id = %s AND tier = %s
      AND team_position = %s
GROUP BY team_position
```

#### 2. `getChampionMatchups()` (line 71)
- 테이블: `champion_matchup_stats_local` → `match_matchup_local`
- 컬럼: `patch` → `patch_version`
- 쿼리: `sum(games)` → `count(*)`, `sum(wins)` → `sum(win)`

변경 전:
```sql
SELECT opponent_champion_id,
       sum(games) AS total_games,
       sum(wins) AS total_wins,
       round(sum(wins) / nullIf(sum(games), 0), 4) AS total_win_rate
FROM champion_matchup_stats_local
WHERE champion_id = %d AND patch = %s AND platform_id = %s AND tier = %s
      AND team_position = %s
GROUP BY opponent_champion_id
ORDER BY total_win_rate DESC, total_games DESC
```

변경 후:
```sql
SELECT opponent_champion_id,
       count(*) AS total_games,
       sum(win) AS total_wins,
       round(sum(win) / nullIf(count(*), 0), 4) AS total_win_rate
FROM match_matchup_local
WHERE champion_id = %d AND patch_version = %s AND platform_id = %s AND tier = %s
      AND team_position = %s
GROUP BY opponent_champion_id
ORDER BY total_win_rate DESC, total_games DESC
```

#### 나머지 메서드 (변경 없음)
`_agg` 테이블을 참조하는 메서드들은 `docs/04_queries.sql`과 일치하므로 변경하지 않음:
- `getChampionRuneBuilds()` → `champion_rune_stats_agg`
- `getChampionSpellStats()` → `champion_spell_stats_agg`
- `getChampionSkillBuilds()` → `champion_skill_build_stats_agg`
- `getChampionStartItemBuilds()` → `champion_start_item_stats_agg`
- `getChampionItemBuilds()` → `champion_item_build_stats_agg`
- `getChampionItemStats()` → `champion_item_stats_agg`, `champion_stats_agg`, `legendary_items`
- `getChampionStatsByPosition()` → `champion_stats_agg`, `match_count_agg`, `champion_bans_agg`

### 테스트 파일 (변경 없음)
경로: `module/infra/persistence/clickhouse/src/test/java/com/example/lolserver/repository/championstats/adapter/ChampionStatsClickHouseAdapterTest.java`

테스트는 `anyString()`으로 SQL을 매칭하므로 테이블명 변경에 영향 없음.

## 검증
- `./gradlew :module:infra:persistence:clickhouse:test` 실행하여 기존 테스트 통과 확인
- `./gradlew build` 실행하여 전체 빌드 성공 확인
