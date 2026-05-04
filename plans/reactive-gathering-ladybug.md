# match_participant LEFT JOIN summoner — gameName/tagLine 우선 사용

## Context

현재 매치 데이터 조회 시 `match_participant` 테이블의 `riot_id_game_name`, `riot_id_tagline`을 직접 사용하고 있다. 이 값은 매치가 플레이된 시점의 스냅샷이므로, 플레이어가 이름을 변경하면 최신 정보가 아닐 수 있다. `summoner` 테이블에 최신 `game_name`, `tag_line`이 있으므로, LEFT JOIN 후 summoner 데이터가 있으면 그 값을 우선 사용하고, 없으면 match_participant의 값으로 fallback한다.

## 영향받는 쿼리 경로 2가지

| 경로 | 호출 흐름 | 사용처 |
|------|----------|--------|
| **Path A (배치)** | `MatchRepositoryCustomImpl.getMatchSummoners(List<String>)` → `MatchSummonerDTO` → `matchMapper.toDomain()` | `MatchPersistenceAdapter.getMatchesBatch()` |
| **Path B (단건)** | `MatchSummonerRepository.findByMatchId()` → `MatchSummonerEntity` → `matchMapper.toDomain()` | `MatchPersistenceAdapter.convertToGameData()` |

## 구현 계획

### Step 1. `MatchRepositoryCustomImpl.getMatchSummoners()` 쿼리에 LEFT JOIN + COALESCE 추가

**파일:** `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/match/dsl/MatchRepositoryCustomImpl.java`

- `QSummonerEntity.summonerEntity` static import 추가
- `Expressions` import 추가
- `.from(matchSummonerEntity)` 뒤에 `.leftJoin(summonerEntity).on(summonerEntity.puuid.eq(matchSummonerEntity.puuid))` 추가
- projection에서 2개 필드를 COALESCE로 교체:
  - `matchSummonerEntity.riotIdGameName` → `Expressions.stringTemplate("COALESCE({0}, {1})", summonerEntity.gameName, matchSummonerEntity.riotIdGameName).as("riotIdGameName")`
  - `matchSummonerEntity.riotIdTagline` → `Expressions.stringTemplate("COALESCE({0}, {1})", summonerEntity.tagLine, matchSummonerEntity.riotIdTagline).as("riotIdTagline")`

### Step 2. `MatchRepositoryCustom` 인터페이스에 단건 오버로드 메서드 추가

**파일:** `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/match/dsl/MatchRepositoryCustom.java`

```java
List<MatchSummonerDTO> getMatchSummoners(String matchId);
```

**파일:** `MatchRepositoryCustomImpl.java`에 구현 추가 — 내부적으로 `getMatchSummoners(List.of(matchId))` 위임

### Step 3. `MatchPersistenceAdapter.convertToGameData()` 경로 변경

**파일:** `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/adapter/MatchPersistenceAdapter.java`

`convertToGameData()` 메서드(line 298-302)에서:
- **Before:** `matchSummonerRepository.findByMatchId()` → entity 매핑
- **After:** `matchRepositoryCustom.getMatchSummoners(matchId)` → DTO 매핑

이렇게 하면 Path A, Path B 모두 동일한 LEFT JOIN + COALESCE 쿼리를 사용하게 됨.

### Step 4. 테스트 수정

#### 4-1. `MatchRepositoryCustomImplTest.java` — LEFT JOIN COALESCE 통합 테스트 추가

**파일:** `module/infra/persistence/postgresql/src/test/java/com/example/lolserver/repository/match/match/dsl/MatchRepositoryCustomImplTest.java`

- `SummonerRepository` 주입 추가
- 테스트 케이스:
  - summoner 존재 시 → summoner의 gameName/tagLine 반환 검증
  - summoner 미존재 시 → match_participant의 riotIdGameName/riotIdTagline fallback 검증

#### 4-2. `MatchPersistenceAdapterTest.java` — Mock 경로 업데이트

**파일:** `module/infra/persistence/postgresql/src/test/java/com/example/lolserver/repository/match/adapter/MatchPersistenceAdapterTest.java`

`convertToGameData()` 호출 경로의 테스트 4개 수정:
- `given(matchSummonerRepository.findByMatchId(...))` → `given(matchRepositoryCustom.getMatchSummoners(matchId))` (DTO 반환)
- `given(matchMapper.toDomain(any(MatchSummonerEntity.class)))` → `given(matchMapper.toDomain(any(MatchSummonerDTO.class)))`

영향 테스트: `getMatches_validParams_returnsGameDataPage`, `getGameData_existingMatchId_returnsGameData`, `getMatches_arenaMode_sortsByPlacement`, `getGameData_withTeamData_returnsGameDataWithTeamInfo`

## 변경하지 않는 파일

- `MatchSummonerDTO.java` — 기존 `riotIdGameName`, `riotIdTagline` setter를 COALESCE 결과가 그대로 사용
- `ParticipantData.java` — 도메인 모델 변경 없음
- `MatchMapper.java` — 기존 `toDomain(MatchSummonerDTO)` 매핑이 그대로 동작
- `SummonerEntity.java`, `MatchSummonerEntity.java` — 변경 없음

## 검증 방법

1. `./gradlew :module:infra:persistence:postgresql:test` — 변경된 통합/단위 테스트 통과 확인
2. `./gradlew build` — 전체 빌드 성공 확인
