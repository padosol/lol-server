# Plan: getMatchBatch 쿼리 병합 분석 및 구현

## Context

`getMatchesBatch`는 현재 5개 SQL 쿼리를 실행합니다 (1 순차 + 4 병렬).
스레드 풀 경합이 성능 병목으로 보고된 상태에서, 병합 가능한 쿼리를 줄이면 병렬 작업 수가 감소하여 성능이 개선됩니다.

## 현재 쿼리 구조 (5개)

| # | 메서드 | 테이블 | 행 수 (20매치 기준) | 컬럼 | 실행 |
|---|--------|--------|-------------------|------|------|
| Q1 | `getMatchDTOs` | match JOIN match_participant | ~20 | 14 | 순차 |
| Q2 | `getMatchSummoners` | match_participant LEFT JOIN summoner | ~200 | 53+ | 병렬 |
| Q3 | `getMatchTeams` | match_team | ~40 | 8 | 병렬 |
| Q4 | `selectItemEventsByMatchIds` | item_events | 수천 | 5 | 병렬 |
| Q5 | `selectSkillEventsByMatchIds` | skill_events | 수천 | 4 | 병렬 |

## 병합 가능성 분석

### ✅ Q2 + Q3 (Participants + Teams) — 병합 추천

- **JOIN 조건**: `match_participant.(match_id, team_id) = match_team.(match_id, team_id)`
- **카디널리티**: 변화 없음 (participant당 정확히 1개 team row 매칭)
- **데이터 증가**: ~200행 × 5개 int = ~4KB (무시 가능)
- **인덱스**: match_team의 unique index `(match_id, team_id)` 활용 → 조인 비용 최소
- **효과**: SQL 5→4, 병렬 작업 4→3

### ❌ Q1 + Q2 (MatchDTOs + Participants) — 병합 불가

- Q1은 매치당 1행, Q2는 매치당 10행 → 카디널리티 불일치
- Q1에 `OFFSET/LIMIT` 페이지네이션이 있어 10배 비정규화 시 페이지네이션 로직이 깨짐
- Q1이 먼저 완료되어야 matchIds를 Q2~Q5에 전달 가능 (순차 의존성)

### ❌ Q2 + Q4/Q5 (Participants + Timeline) — 병합 불가

- 타임라인 이벤트는 참가자당 수십~수백 행 (아이템 구매/스킬 이벤트 각각)
- JOIN 시 Cartesian product: 200 participants × 수천 events = 수십만 행
- 완전히 다른 데이터 세분도(granularity)

### ⚠️ Q4 + Q5 (Items + Skills) — 기술적으로 가능하나 비추천

- UNION ALL로 합칠 수 있으나 스키마가 다름 (5 vs 4 컬럼)
- NULL 패딩 + type 구분 컬럼 필요 → 파싱 복잡성 증가
- 각각 단순 SELECT이므로 DB 측 비용 차이 미미
- 코드 복잡성 증가 대비 이득이 작음

### ❌ Q1 + Q3 (MatchDTOs + Teams) — 병합 불가

- Q1은 매치당 1행, Q3은 매치당 2행 → 페이지네이션 로직 깨짐

## 결론: Q2 + Q3 병합만 실용적

---

## 구현 계획 (Q2 + Q3 병합)

### 수정 대상 파일

| 파일 | 변경 내용 |
|------|-----------|
| `.../dto/MatchSummonerDTO.java` | 팀 집계 필드 5개 추가 |
| `.../match/dsl/MatchRepositoryCustomImpl.java` | LEFT JOIN match_team 추가, projection에 팀 컬럼 추가, `getMatchTeams` 제거 |
| `.../match/dsl/MatchRepositoryCustom.java` | `getMatchTeams` 인터페이스 메서드 제거 |
| `.../adapter/MatchPersistenceAdapter.java` | teamsFuture 제거, participant에서 팀 정보 추출 |
| `.../mapper/MatchMapper.java` | `toDomain(MatchTeamDTO)` 제거 (dead code) |

### Step 1: MatchSummonerDTO에 팀 필드 추가

**파일:** `.../repository/match/dto/MatchSummonerDTO.java`

`team` prefix로 5개 필드 추가 (participant 자체의 `baronKills` 등과 이름 충돌 방지):

```java
// 팀 집계 정보 (match_team LEFT JOIN)
private int teamChampionKills;
private int teamBaronKills;
private int teamDragonKills;
private int teamTowerKills;
private int teamInhibitorKills;
```

### Step 2: QueryDSL 쿼리 수정

**파일:** `.../match/dsl/MatchRepositoryCustomImpl.java`

**2a.** `getMatchSummoners`에 LEFT JOIN 추가:
```java
.leftJoin(matchTeamEntity)
    .on(matchTeamEntity.matchId.eq(matchSummonerEntity.matchId)
        .and(matchTeamEntity.teamId.eq(matchSummonerEntity.teamId)))
```

**2b.** `matchSummonerProjection()`에 팀 컬럼 alias 추가:
```java
matchTeamEntity.championKills.as("teamChampionKills"),
matchTeamEntity.baronKills.as("teamBaronKills"),
matchTeamEntity.dragonKills.as("teamDragonKills"),
matchTeamEntity.towerKills.as("teamTowerKills"),
matchTeamEntity.inhibitorKills.as("teamInhibitorKills")
```

### Step 3: MatchPersistenceAdapter 수정

**3a.** `getMatchesBatch`에서 `teamsFuture` CompletableFuture 및 `teamsByMatch` 제거

**3b.** `assembleGameDataFromDTO` 시그니처에서 `List<MatchTeamDTO> teamDTOs` 파라미터 제거

**3c.** 팀 정보를 participant에서 추출하는 로직으로 변경:
```java
if (!summonerDTOs.isEmpty()) {
    TeamInfoData blueTeam = null;
    TeamInfoData redTeam = null;
    for (MatchSummonerDTO dto : summonerDTOs) {
        if (dto.getTeamId() == 100 && blueTeam == null)
            blueTeam = toTeamInfoData(dto);
        else if (dto.getTeamId() == 200 && redTeam == null)
            redTeam = toTeamInfoData(dto);
        if (blueTeam != null && redTeam != null) break;
    }
    gameData.setTeamInfoData(TeamData.builder()
            .blueTeam(blueTeam).redTeam(redTeam).build());
}
```

**3d.** `toTeamInfoData` 헬퍼 메서드 추가

### Step 4: Dead code 제거

- `MatchRepositoryCustomImpl.getMatchTeams()` 삭제
- `MatchRepositoryCustom` 인터페이스에서 `getMatchTeams` 삭제
- `MatchMapper.toDomain(MatchTeamDTO dto)` 삭제
- 미사용 import 정리

### Step 5: 빌드 검증

```bash
./gradlew clean build
```
