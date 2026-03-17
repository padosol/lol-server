# getMatchesBatch 쿼리 통합 분석 및 계획

## Context

`getMatchesBatch`는 현재 5개의 순차 쿼리를 실행합니다:
1. `getMatchDTOs` - 페이지네이션된 match 조회 (match JOIN match_summoner)
2. `getMatchSummoners` - match_summoner WHERE match_id IN (...)  → 매치당 10행
3. `getMatchTeams` - match_team WHERE match_id IN (...)  → 매치당 2행
4. `selectItemEventsByMatchIds` - item_events JOIN time_line_event  → 매치당 N행
5. `selectSkillEventsByMatchIds` - skill_events JOIN time_line_event  → 매치당 N행

사용자 요청: (1+2+3) → 1쿼리, (4+5) → 1쿼리로 통합

---

## 분석: match + match_summoner + match_team 통합

### 문제점: Cartesian Product

세 테이블을 JOIN하면 **Cartesian product** 발생:
- match × match_summoner × match_team = 1 × 10 × 2 = **매치당 20행**
- 20매치 기준: **400행** (현재 개별 쿼리 합계: 20 + 200 + 40 = **260행**)

match 데이터(14컬럼)가 20번, team 데이터(18컬럼)가 10번 중복 전송됩니다.

### QueryDSL GroupBy.groupBy() 사용 시

```java
queryFactory
    .from(matchEntity)
    .leftJoin(matchSummonerEntity).on(...)
    .leftJoin(matchTeamEntity).on(...)
    .where(matchEntity.matchId.in(matchIds))
    .transform(GroupBy.groupBy(matchEntity.matchId).as(...));
```

- DB → App 데이터 전송량: **증가** (Cartesian product는 DB 레벨에서 발생)
- GroupBy.set()으로 중복 제거하려면 DTO에 equals/hashCode 구현 필요
- 네트워크 라운드트립: 2회 절약
- **결론: 쿼리 수는 줄지만, 전송 데이터량이 늘어 오히려 비효율적**

---

## 분석: item_events + skill_events 통합

### UNION ALL 사용 (Native Query)

두 이벤트 DTO 구조가 유사:

| 필드 | ItemEventDTO | SkillEventDTO |
|------|-------------|---------------|
| matchId | O | O |
| participantId | O | O |
| timestamp | O | O |
| type | O | O |
| **itemId** | O | - |
| **skillSlot** | - | O |

UNION ALL로 합칠 수 있지만 QueryDSL은 UNION을 지원하지 않아 **Native Query** 필요.
기존의 QueryDSL 기반 패턴과 일관성이 깨지며, 결과를 파싱하는 복잡도가 추가됩니다.

---

## 권장 접근: 쿼리 병렬 실행

쿼리를 물리적으로 합치는 대신, 독립적인 쿼리 4개(2~5)를 **병렬로 실행**하면:

- **현재**: Query1 → Query2 → Query3 → Query4 → Query5 (순차)
- **변경 후**: Query1 → max(Query2, Query3, Query4, Query5) (병렬)

### 구현 방법: CompletableFuture

```java
// Query 1: 페이지네이션 (순차 - matchIds 필요)
List<String> matchIds = ...;

// Query 2~5: 병렬 실행
CompletableFuture<Map<String, List<MatchSummonerDTO>>> summonersFuture =
    CompletableFuture.supplyAsync(() ->
        matchRepositoryCustom.getMatchSummoners(matchIds).stream()
            .collect(Collectors.groupingBy(MatchSummonerDTO::getMatchId)));

CompletableFuture<Map<String, List<MatchTeamDTO>>> teamsFuture = ...;
CompletableFuture<Map<String, List<ItemEventDTO>>> itemsFuture = ...;
CompletableFuture<Map<String, List<SkillEventDTO>>> skillsFuture = ...;

CompletableFuture.allOf(summonersFuture, teamsFuture, itemsFuture, skillsFuture).join();
```

### 주의사항
- JPA EntityManager는 스레드 안전하지 않음
- `@Transactional` 내에서 CompletableFuture를 사용하면 각 스레드가 별도 트랜잭션/커넥션 사용
- 읽기 전용 쿼리이므로 트랜잭션 격리 문제는 사실상 없음 (데이터가 방금 커밋된 상태)
- DB 커넥션 풀 부하가 기존 1개 → 4개로 증가

---

## 채택: 병렬 실행

---

## 구현 계획

### 수정 파일
- `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/adapter/MatchPersistenceAdapter.java`

### 변경 내용

`getMatchesBatch` 메서드에서 Query 2~5를 `CompletableFuture`로 병렬 실행:

```java
@Override
public Page<GameReadModel> getMatchesBatch(
        String puuid, Integer queueId, Pageable pageable
) {
    // Query 1: 페이지네이션 (순차 - matchIds 추출 필요)
    Slice<MatchDTO> matchesSlice =
            matchRepositoryCustom.getMatchDTOs(puuid, queueId, pageable);
    List<MatchDTO> matchDTOs = matchesSlice.getContent();

    if (matchDTOs.isEmpty()) {
        return new Page<>(Collections.emptyList(), false);
    }

    List<String> matchIds = matchDTOs.stream()
            .map(MatchDTO::getMatchId)
            .toList();

    // Query 2~5: 병렬 실행
    CompletableFuture<Map<String, List<MatchSummonerDTO>>> summonersFuture =
        CompletableFuture.supplyAsync(() ->
            matchRepositoryCustom.getMatchSummoners(matchIds).stream()
                .collect(Collectors.groupingBy(MatchSummonerDTO::getMatchId)));

    CompletableFuture<Map<String, List<MatchTeamDTO>>> teamsFuture =
        CompletableFuture.supplyAsync(() ->
            matchRepositoryCustom.getMatchTeams(matchIds).stream()
                .collect(Collectors.groupingBy(MatchTeamDTO::getMatchId)));

    CompletableFuture<Map<String, List<ItemEventDTO>>> itemsFuture =
        CompletableFuture.supplyAsync(() ->
            timelineRepositoryCustom.selectItemEventsByMatchIds(matchIds).stream()
                .collect(Collectors.groupingBy(ItemEventDTO::getMatchId)));

    CompletableFuture<Map<String, List<SkillEventDTO>>> skillsFuture =
        CompletableFuture.supplyAsync(() ->
            timelineRepositoryCustom.selectSkillEventsByMatchIds(matchIds).stream()
                .collect(Collectors.groupingBy(SkillEventDTO::getMatchId)));

    CompletableFuture.allOf(
        summonersFuture, teamsFuture, itemsFuture, skillsFuture
    ).join();

    Map<String, List<MatchSummonerDTO>> participantsByMatch = summonersFuture.join();
    Map<String, List<MatchTeamDTO>> teamsByMatch = teamsFuture.join();
    Map<String, List<ItemEventDTO>> itemEventsByMatch = itemsFuture.join();
    Map<String, List<SkillEventDTO>> skillEventsByMatch = skillsFuture.join();

    // 이하 조립 로직 동일
    ...
}
```

### 핵심 포인트
- 기존 쿼리 메서드 변경 없음 (실행 방식만 순차→병렬)
- `JPAQueryFactory`는 내부적으로 `EntityManager`를 Provider로 관리하므로, 각 스레드에서 별도 EntityManager 획득
- 읽기 전용 쿼리이므로 트랜잭션 격리 이슈 없음
- DB 커넥션 풀에서 동시에 최대 4개 커넥션 사용 (기존 1개 순차 사용 대비)

### 검증
- `./gradlew test` 전체 테스트 통과 확인
- 반환 데이터 동일성 보장 (쿼리 로직 변경 없음)
