# 소환사별 게임 데이터 리스트 API에 season 파라미터 추가

## Context

`GET /{platformId}/summoners/{puuid}/matches` API에서 `season` 파라미터가 누락되어 있습니다.
같은 컨트롤러의 다른 API(`daily-count`, `rank/champions`)에서는 이미 season 필터링이 구현되어 있으나,
소환사별 매치 목록 배치 조회 API만 누락된 상태입니다. 이로 인해 시즌별 매치 필터링이 불가능합니다.

## 수정 대상 파일 (안쪽→바깥쪽 순서)

### 1. `MatchCommand` - season 필드 추가
**파일**: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/match/application/command/MatchCommand.java`

```java
private Integer season;  // 필드 추가
```

### 2. `MatchPersistencePort` - 포트 메서드 시그니처 변경
**파일**: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/match/application/port/out/MatchPersistencePort.java`

- `getMatchesBatch(String puuid, Integer queueId, Pageable pageable)` → `getMatchesBatch(String puuid, Integer season, Integer queueId, Pageable pageable)`

### 3. `MatchService` - season 파라미터 전달
**파일**: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/match/application/MatchService.java`

`getMatchesBatch()` 메서드에서 `matchCommand.getSeason()`을 포트에 전달:
```java
return matchPersistencePort.getMatchesBatch(
    matchCommand.getPuuid(), matchCommand.getSeason(), matchCommand.getQueueId(), pageable);
```

### 4. `MatchRepositoryCustom` - 리포지토리 인터페이스 변경
**파일**: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/match/dsl/MatchRepositoryCustom.java`

- `getMatchDTOs(String puuid, Integer queueId, Pageable pageable)` → `getMatchDTOs(String puuid, Integer season, Integer queueId, Pageable pageable)`

### 5. `MatchRepositoryCustomImpl` - QueryDSL 쿼리에 season 필터 추가
**파일**: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/match/dsl/MatchRepositoryCustomImpl.java`

`getMatchDTOs()` 메서드의 WHERE 절에 `seasonEq(season)` 조건 추가.
`seasonEq` 헬퍼 메서드 추가 (기존 `MatchSummonerRepositoryCustomImpl`과 동일한 패턴):
```java
private BooleanExpression seasonEq(Integer season) {
    return season != null ? matchEntity.season.eq(season) : null;
}
```
> `season`이 `null`일 경우 필터를 적용하지 않도록 처리 (선택적 파라미터)

### 6. `MatchPersistenceAdapter` - 어댑터 메서드 시그니처 변경
**파일**: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/adapter/MatchPersistenceAdapter.java`

`getMatchesBatch()` 메서드에 `season` 파라미터 추가 후 `matchRepositoryCustom.getMatchDTOs()`에 전달.

### 7. `MatchController` - API 파라미터 추가
**파일**: `module/infra/api/src/main/java/com/example/lolserver/controller/match/MatchController.java`

`fetchMatchesBySummoner()` 메서드에 `@RequestParam(required = false) Integer season` 추가 후 `MatchCommand`에 설정.

### 8. `MatchControllerTest` - RestDocs 테스트 업데이트
**파일**: `module/infra/api/src/test/java/com/example/lolserver/docs/controller/MatchControllerTest.java`

`fetchMatchesBySummoner()` 테스트에 `.param("season", "2025")` 추가 및 `queryParameters`에 season 문서화 추가.

## 검증 방법

```bash
./gradlew test
```
- `MatchControllerTest.fetchMatchesBySummoner()` 테스트 통과 확인
- 전체 빌드 성공 확인: `./gradlew build`
