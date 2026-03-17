# getMatchSummoners + getMatchTeams 단일 쿼리 병합

## Context

`getMatchesBatch`는 현재 5개의 DB 쿼리를 실행합니다. 그 중 쿼리 2(`getMatchSummoners`)와 쿼리 3(`getMatchTeams`)은 동일한 `matchIds IN (...)` 조건으로 조회하므로, LEFT JOIN으로 합쳐 **DB 라운드트립을 1회 줄입니다** (5쿼리 → 4쿼리).

`MatchSummonerEntity.teamId`와 `MatchTeamEntity.(matchId, teamId)` unique constraint를 활용하면 소환사당 1:1 JOIN이 되어 카테시안 곱 문제가 없습니다.

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `repository/match/dto/MatchSummonersAndTeamsResult.java` | **신규** - 결과 홀더 record |
| `repository/match/match/dsl/MatchRepositoryCustom.java` | `getMatchSummoners`, `getMatchTeams` 제거 → `getMatchSummonersWithTeams` 추가 |
| `repository/match/match/dsl/MatchRepositoryCustomImpl.java` | 2개 메서드 제거, Tuple 기반 단일 쿼리 구현 |
| `repository/match/adapter/MatchPersistenceAdapter.java` | `getMatchesBatch()`에서 새 메서드 호출로 변경 |
| `repository/match/adapter/MatchPersistenceAdapterTest.java` | `getMatchesBatch` 테스트 추가 |

## 구현 순서

### 1. `MatchSummonersAndTeamsResult` record 생성

```java
package com.example.lolserver.repository.match.dto;

public record MatchSummonersAndTeamsResult(
        List<MatchSummonerDTO> summoners,
        List<MatchTeamDTO> teams
) {}
```

### 2. `MatchRepositoryCustom` 인터페이스 변경

- 제거: `List<MatchSummonerDTO> getMatchSummoners(List<String> matchIds)`
- 제거: `List<MatchTeamDTO> getMatchTeams(List<String> matchIds)`
- 추가: `MatchSummonersAndTeamsResult getMatchSummonersWithTeams(List<String> matchIds)`

### 3. `MatchRepositoryCustomImpl` 구현

- `matchSummonerEntity LEFT JOIN matchTeamEntity ON (matchId, teamId)` 단일 쿼리
- `Tuple` 반환 → summoners 직접 추출, teams는 `LinkedHashMap`으로 `matchId_teamId` 키 기반 중복 제거
- 기존 `itemProjection()`, `statValueProjection()`, `styleValueProjection()` 헬퍼 재사용

생성 SQL:
```sql
SELECT ms.*, mt.*
FROM match_summoner ms
LEFT JOIN match_team mt ON mt.match_id = ms.match_id AND mt.team_id = ms.team_id
WHERE ms.match_id IN (:matchIds)
```

### 4. `MatchPersistenceAdapter.getMatchesBatch()` 수정

```java
// Before: 2개 쿼리
matchRepositoryCustom.getMatchSummoners(matchIds)
matchRepositoryCustom.getMatchTeams(matchIds)

// After: 1개 쿼리
MatchSummonersAndTeamsResult result =
        matchRepositoryCustom.getMatchSummonersWithTeams(matchIds);
// result.summoners() → participantsByMatch
// result.teams() → teamsByMatch
```

나머지 `assembleGameDataFromDTO` 로직은 변경 없음.

### 5. 테스트 추가

`MatchPersistenceAdapterTest`에 `getMatchesBatch` 테스트 추가 (기존에 없음). 새 메서드 mock 설정.

## 검증

```bash
./gradlew test
./gradlew build   # checkstyle 포함
```
