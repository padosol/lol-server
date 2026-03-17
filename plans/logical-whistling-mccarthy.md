# daily-count API "argument type mismatch" 에러 수정

## Context

`/api/v1/summoners/{puuid}/matches/daily-count?season=26&queueId=420` 요청 시 "argument type mismatch" 런타임 에러 발생.

이전 수정(`Long` → `long`)으로 해결되지 않음. `QMSChampionDTO`도 동일한 boxing 패턴(`double.class` vs `Double`, `long.class` vs `Long`)인데 정상 동작하므로, boxing/unboxing 불일치가 근본 원인이 아님.

실제 원인은 `ConstructorExpression` + `DateTemplate<LocalDate>`의 `CAST({0} AS DATE)` 조합에서 런타임 타입 리졸빙 문제로 추정됨. Q클래스 기반 ConstructorExpression을 제거하면 이 문제를 근본적으로 회피할 수 있음.

## 수정 방법

**Q클래스(`QDailyGameCountDTO`) 의존 제거 → Tuple 기반 쿼리로 전환**

### 1. `DailyGameCountDTO.java` 수정

**파일**: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/dto/DailyGameCountDTO.java`

- `@QueryProjection` 제거
- `gameCount` 타입을 `Long` (boxed)으로 복원

```java
@Getter
public class DailyGameCountDTO {

    private LocalDate gameDate;
    private Long gameCount;

    public DailyGameCountDTO(LocalDate gameDate, Long gameCount) {
        this.gameDate = gameDate;
        this.gameCount = gameCount;
    }
}
```

### 2. `MatchSummonerRepositoryCustomImpl.findDailyGameCounts()` 수정

**파일**: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/matchsummoner/dsl/impl/MatchSummonerRepositoryCustomImpl.java`

- `QDailyGameCountDTO` 대신 Tuple 쿼리 사용
- `QDailyGameCountDTO` import 제거

```java
@Override
public List<DailyGameCountDTO> findDailyGameCounts(
        String puuid, Integer season, Integer queueId, LocalDateTime startDate) {

    DateTemplate<LocalDate> gameDate = Expressions.dateTemplate(
            LocalDate.class, "CAST({0} AS DATE)", matchEntity.gameCreateDatetime);

    return jpaQueryFactory
            .select(gameDate, matchSummonerEntity.count())
            .from(matchSummonerEntity)
            .join(matchEntity).on(matchEntity.matchId.eq(matchSummonerEntity.matchId))
            .where(
                    puuidEq(puuid),
                    seasonEq(season),
                    queueIdEqOrAll(queueId),
                    matchEntity.gameCreateDatetime.goe(startDate)
            )
            .groupBy(gameDate)
            .orderBy(gameDate.asc())
            .fetch()
            .stream()
            .map(tuple -> new DailyGameCountDTO(
                    tuple.get(gameDate),
                    tuple.get(matchSummonerEntity.count())))
            .toList();
}
```

### 3. 미사용 import 정리

`MatchSummonerRepositoryCustomImpl.java`에서 `QDailyGameCountDTO` import 제거.

## 검증

1. `./gradlew clean build` — 전체 빌드 및 테스트 통과 확인
2. 로컬 서버 실행 후 `GET /api/v1/summoners/{puuid}/matches/daily-count?season=26&queueId=420` 호출하여 정상 응답 확인
