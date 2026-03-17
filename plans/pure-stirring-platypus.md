# ClickHouse JDBC 컬럼명 매칭 오류 수정

## Context

`getChampionStatsByPosition` 메서드 실행 시 `Column [team_position] does not exist in 5 columns` 오류가 발생한다.

**원인**: SELECT 절에서 `s.team_position`, `s.champion_id`처럼 테이블 별칭(alias) 접두사를 사용하면, ClickHouse JDBC 드라이버가 결과 컬럼명을 `s.team_position`으로 반환한다. 이후 RowMapper에서 `rs.getString("team_position")`으로 접근하면 컬럼을 찾지 못해 오류가 발생한다.

동일 파일의 다른 메서드들(`getChampionWinRates` 등)은 별칭 없이 `SELECT team_position, ...`을 사용하여 정상 동작한다.

## 수정 방법

### 파일: `module/infra/persistence/clickhouse/src/main/java/com/example/lolserver/repository/championstats/adapter/ChampionStatsClickHouseAdapter.java`

**Line 229-230**: SELECT 절에 명시적 컬럼 별칭(AS) 추가

```sql
-- Before
SELECT s.team_position, s.champion_id,

-- After
SELECT s.team_position AS team_position, s.champion_id AS champion_id,
```

이렇게 하면 JDBC ResultSet의 컬럼명이 `team_position`, `champion_id`로 고정되어 RowMapper에서 정상적으로 접근 가능하다.

## 검증

1. `./gradlew :module:infra:persistence:clickhouse:test` 실행하여 기존 테스트 통과 확인
2. `./gradlew build` 전체 빌드 확인
