# getMatchesBatch() ReadModel DTO 프로젝션 최적화

> DTO 명명: `*Batch*` 접두/접미사 제거 — 간결한 이름 사용

## Context

`getMatchesBatch()` 메서드는 5개 쿼리로 전체 JPA 엔티티를 로드하지만, 실제 응답에 사용되는 필드는 일부입니다.
특히 `MatchSummonerEntity`는 197개 필드 중 62개만 사용 (31.5%).
ReadModel DTO를 만들어 QueryDSL 프로젝션으로 필요한 컬럼만 SELECT합니다.

## 필드 사용 분석

| 엔티티 | 전체 | 사용 | 절감 |
|--------|------|------|------|
| **MatchSummonerEntity** | 197 | 62 | **69%** |
| MatchEntity | 26 | 14 | 46% |
| MatchTeamEntity | 20 | 18 | 10% |
| ItemEventsEntity | 9 | 5 | 44% |
| SkillEventsEntity | 6 | 5 | 17% |

## 수정 계획

### Phase 1: DTO 생성 (5개 파일)

모든 DTO 위치: `repository/match/dto/`

#### 1.1 `MatchDTO` (14 fields, `@QueryProjection`)
- matchId, dataVersion, gameCreation, gameDuration, gameEndTimestamp, gameStartTimestamp
- gameMode, gameType, gameVersion, mapId, queueId, platformId, tournamentCode, averageTier

#### 1.2 `MatchSummonerDTO` (~62 fields, `Projections.bean()`)
- 48 스칼라 필드 (puuid, matchId, kills, deaths, assists 등)
- 3 임베디드 값객체: `ItemValue item`, `StatValue statValue`, `StyleValue styleValue`
  - 임베디드 타입은 persistence-layer `@Embeddable` 타입 재사용
  - QueryDSL에서 `Projections.bean(ItemValue.class, ...).as("item")` 형태로 중첩 프로젝션
  - **Fallback**: 중첩 프로젝션 실패 시 14개 임베디드 필드를 flat하게 펼쳐서 매퍼에서 재조립

#### 1.3 `MatchTeamDTO` (18 fields, `@QueryProjection`)
- matchId, teamId, win, championKills, baronKills, dragonKills, towerKills, inhibitorKills
- champion1Id~5Id, pick1Turn~5Turn

#### 1.4 `ItemEventDTO` (5 fields, `@QueryProjection`)
- matchId (TimeLineEventEntity에서 JOIN), itemId, participantId, timestamp, type

#### 1.5 `SkillEventDTO` (5 fields, `@QueryProjection`)
- matchId (TimeLineEventEntity에서 JOIN), skillSlot, participantId, timestamp, type

### Phase 2: 레포지토리 쿼리 추가

#### 2.1 `MatchRepositoryCustom` — 3개 메서드 추가

기존 `getMatches()` 유지 (`adapter.getMatches()`에서도 사용 중).

```java
// 새 메서드 추가
Slice<MatchDTO> getMatchDTOs(String puuid, Integer queueId, Pageable pageable);
List<MatchSummonerDTO> getMatchSummoners(List<String> matchIds);
List<MatchTeamDTO> getMatchTeams(List<String> matchIds);
```

#### 2.2 `TimelineRepositoryCustom` — 2개 메서드 추가

기존 `selectAllItemEventsByMatchIds()` 유지 (다른 곳에서 사용 가능).

```java
List<ItemEventDTO> selectItemEventsByMatchIds(List<String> matchIds);
List<SkillEventDTO> selectSkillEventsByMatchIds(List<String> matchIds);
```

- `fetchJoin()` 제거 — 프로젝션이므로 엔티티 그래프 로딩 불필요
- `timeLineEventEntity.matchId`를 직접 SELECT하여 DTO에 포함

### Phase 3: 매퍼 추가

`MatchMapper.java`에 새 매핑 메서드 추가:

```java
// DTO → Domain 매핑
@Mapping(target = "averageTier", source = "averageTier", qualifiedByName = "mapAverageTierToString")
@Mapping(target = "averageRank", source = "averageTier", qualifiedByName = "mapAverageTierToRank")
GameInfoData toGameInfoData(MatchDTO dto);

@Mapping(target = "style", source = "styleValue")  // 필드명 불일치 해결
ParticipantData toDomain(MatchSummonerDTO dto);
// MapStruct가 기존 toDomain(ItemValue), toDomain(StatValue), toDomain(StyleValue) 자동 발견

@Mapping(target = "championId", expression = "java(mapChampionIds(dto))")
@Mapping(target = "pickTurn", expression = "java(mapPickTurns(dto))")
TeamInfoData toDomain(MatchTeamDTO dto);

ItemEvents toDomain(ItemEventDTO dto);
SkillEvents toDomain(SkillEventDTO dto);
List<ItemEvents> toDomainItemEventDTOList(List<ItemEventDTO> dtos);
List<SkillEvents> toDomainSkillEventDTOList(List<SkillEventDTO> dtos);
```

`mapChampionIds(MatchTeamDTO)`, `mapPickTurns(MatchTeamDTO)` default 메서드 추가.

**참고**: 기존 `toDomain(MatchSummonerEntity)` → `ParticipantData` 매핑에서 `styleValue` → `style` 매핑이 누락되어 있음 (필드명 불일치). DTO 매핑에서는 `@Mapping(target = "style", source = "styleValue")`로 이 문제를 해결함.

### Phase 4: 어댑터 수정

`MatchPersistenceAdapter.getMatchesBatch()`:
- `matchRepositoryCustom.getMatchDTOs()` 사용
- `matchRepositoryCustom.getMatchSummoners()` 사용
- `matchRepositoryCustom.getMatchTeams()` 사용
- `timelineRepositoryCustom.selectItemEventsByMatchIds()` 사용
- `timelineRepositoryCustom.selectSkillEventsByMatchIds()` 사용
- 새 `assembleGameDataFromDTO()` private 메서드 추가

기존 `getMatches()`, `convertToGameData()` 등은 변경 없음.

## 수정 파일 목록

| # | 파일 | 액션 |
|---|------|------|
| 1 | `repository/match/dto/MatchDTO.java` | **생성** |
| 2 | `repository/match/dto/MatchSummonerDTO.java` | **생성** |
| 3 | `repository/match/dto/MatchTeamDTO.java` | **생성** |
| 4 | `repository/match/dto/ItemEventDTO.java` | **생성** |
| 5 | `repository/match/dto/SkillEventDTO.java` | **생성** |
| 6 | `repository/match/match/dsl/MatchRepositoryCustom.java` | **수정** — 3개 메서드 추가 |
| 7 | `repository/match/match/dsl/MatchRepositoryCustomImpl.java` | **수정** — 3개 프로젝션 쿼리 구현 |
| 8 | `repository/match/timeline/TimelineRepositoryCustom.java` | **수정** — 2개 메서드 추가 |
| 9 | `repository/match/timeline/TimelineRepositoryCustomImpl.java` | **수정** — 2개 프로젝션 쿼리 구현 |
| 10 | `repository/match/mapper/MatchMapper.java` | **수정** — DTO→Domain 매핑 추가 |
| 11 | `repository/match/adapter/MatchPersistenceAdapter.java` | **수정** — getMatchesBatch DTO 사용 |

모든 경로 prefix: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/`

## 구현 순서

1. DTO 5개 생성 → `./gradlew compileJava` (Q-class 생성)
2. 레포지토리 인터페이스 + 구현체
3. MatchMapper 매핑 메서드 추가
4. MatchPersistenceAdapter 수정

## 검증

1. `./gradlew build` — 컴파일, checkstyle, 테스트 통과
2. `./gradlew :module:infra:persistence:postgresql:test` — 기존 테스트 통과
3. 기능 확인: `getMatchesBatch()` 반환 데이터 동일, `hasNext()` 정상 동작
