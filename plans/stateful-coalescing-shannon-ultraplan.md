# participant_frame 조회 쿼리 추가 — 분당 골드 데이터 제공

## Context

분당 골드량(gold per minute) 시계열 데이터를 매치 상세 조회에 포함시키기 위해, `participant_frame` 테이블 조회가 필요하다. 현재 `ParticipantFrameEntity`는 JPA 엔티티만 존재하고 **리포지토리도, 조회 쿼리도, 도메인 참조도 전혀 없는** 상태이다.

사용자가 제안한 4-테이블 JOIN SQL:
```sql
select * from match_participant mp
left join summoner s on mp.puuid = s.puuid
join match_team mt on mt.match_id = mp.match_id and mp.team_id = mt.team_id
join participant_frame pf on mp.match_id = pf.match_id and mp.participant_id = pf.participant_id
where mp.match_id = 'KR_8034378159'
```

### 드래프트 플랜 정정

드래프트 플랜에서 `getMatchSummoners()`가 3개 테이블 JOIN을 한다고 기술했으나, 실제 코드(`MatchRepositoryCustomImpl:127-186`)는 `matchSummonerEntity` **단일 테이블만** 조회한다. summoner, team, itemEvents, skillEvents는 모두 별도 쿼리 + `CompletableFuture` 병렬 실행으로 조합된다.

## 방향: 쿼리 분리 (기존 패턴 준수)

단일 4-테이블 JOIN 대신, 기존 `getMatchesBatch()` 패턴처럼 **독립 쿼리 + Java 조합** 방식을 따른다.

```
┌─────────────────────────────────────┐
│      MatchPersistenceAdapter        │
│      getMatchesBatch()              │
├─────────────────────────────────────┤
│  CompletableFuture.allOf(           │
│    summonersFuture,   ── 기존       │
│    teamsFuture,       ── 기존       │
│    itemsFuture,       ── 기존       │
│    skillsFuture,      ── 기존       │
│    framesFuture       ── 신규 ★     │
│  ).join()                           │
│                                     │
│  assembleGameDataFromDTO(           │
│    ..., framesByMatch  ── 신규 ★    │
│  )                                  │
└─────────────────────────────────────┘
```

**이유:**
1. 참가자 10명 x ~40 타임스탬프 = ~400행 — participant 데이터(~50컬럼)가 40배 중복되는 낭비 방지
2. 기존 코드베이스가 이미 쿼리 분리 + CompletableFuture 병렬 패턴 사용 중 (일관성)
3. participant_frame 조회를 독립 쿼리로 만들면 다른 기능에서도 재사용 가능

## 구현 계획

### Step 1: `ParticipantFrameDTO` 생성

**파일**: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/dto/ParticipantFrameDTO.java`

QueryDSL `@QueryProjection` 기반 DTO. `totalGold`와 `timestamp`, `participantId`, `matchId`만 포함 (분당 골드 계산에 필요한 최소 필드). 필요 시 추가 필드를 포함하되, 불필요한 embedded value 전체 복사는 지양.

```java
@Getter @Setter @NoArgsConstructor
public class ParticipantFrameDTO {
    private String matchId;
    private int participantId;
    private int timestamp;
    private int totalGold;
    private int minionsKilled;
    private int jungleMinionsKilled;
    private int level;
    private int xp;

    @QueryProjection
    public ParticipantFrameDTO(String matchId, int participantId, int timestamp,
                                int totalGold, int minionsKilled, int jungleMinionsKilled,
                                int level, int xp) { ... }
}
```

### Step 2: `TimelineRepositoryCustom`에 메서드 추가

**파일**: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/timeline/TimelineRepositoryCustom.java`

```java
List<ParticipantFrameDTO> selectParticipantFramesByMatchIds(List<String> matchIds);
```

### Step 3: `TimelineRepositoryCustomImpl`에 쿼리 구현

**파일**: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/timeline/TimelineRepositoryCustomImpl.java`

```java
import static com.example.lolserver.repository.match.entity.timeline.QParticipantFrameEntity.participantFrameEntity;

@Override
public List<ParticipantFrameDTO> selectParticipantFramesByMatchIds(List<String> matchIds) {
    return jpaQueryFactory
        .select(new QParticipantFrameDTO(
            participantFrameEntity.matchId,
            participantFrameEntity.participantId,
            participantFrameEntity.timestamp,
            participantFrameEntity.totalGold,
            participantFrameEntity.minionsKilled,
            participantFrameEntity.jungleMinionsKilled,
            participantFrameEntity.level,
            participantFrameEntity.xp
        ))
        .from(participantFrameEntity)
        .where(participantFrameEntity.matchId.in(matchIds))
        .orderBy(participantFrameEntity.timestamp.asc())
        .fetch();
}
```

### Step 4: 도메인 모델 추가 — `ParticipantFrameData`

**파일**: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/match/domain/gamedata/timeline/ParticipantFrameData.java`

도메인 레이어에 인프라 의존 없는 순수 데이터 클래스. Java record 권장:

```java
public record ParticipantFrameData(
    int timestamp,
    int totalGold,
    int minionsKilled,
    int jungleMinionsKilled,
    int level,
    int xp
) {}
```

### Step 5: `ParticipantData`에 프레임 데이터 필드 추가

**파일**: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/match/domain/gamedata/ParticipantData.java`

```java
private List<ParticipantFrameData> frames;
```

### Step 6: `MatchMapper`에 매핑 메서드 추가

**파일**: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/mapper/MatchMapper.java`

```java
ParticipantFrameData toDomain(ParticipantFrameDTO dto);
List<ParticipantFrameData> toDomainFrameList(List<ParticipantFrameDTO> dtos);
```

### Step 7: `MatchPersistenceAdapter.getMatchesBatch()`에 통합

**파일**: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/adapter/MatchPersistenceAdapter.java`

`getMatchesBatch()` 메서드 (line 105-183)에 5번째 `CompletableFuture` 추가:

```java
CompletableFuture<Map<String, List<ParticipantFrameDTO>>> framesFuture =
    CompletableFuture.supplyAsync(() ->
        timelineRepositoryCustom
            .selectParticipantFramesByMatchIds(matchIds)
            .stream()
            .collect(Collectors.groupingBy(ParticipantFrameDTO::getMatchId)));
```

`assembleGameDataFromDTO()`에 `Map<String, List<ParticipantFrameDTO>> framesByMatch` 파라미터 추가. 내부에서 `participantId` 기준 그룹핑 후 `ParticipantData.setFrames()` 호출:

```java
Map<Integer, List<ParticipantFrameDTO>> framesByParticipant = frameDTOs.stream()
    .collect(Collectors.groupingBy(ParticipantFrameDTO::getParticipantId));

for (ParticipantData participant : participantDataList) {
    List<ParticipantFrameDTO> participantFrames =
        framesByParticipant.getOrDefault(participant.getParticipantId(), Collections.emptyList());
    participant.setFrames(
        matchMapper.toDomainFrameList(participantFrames));
}
```

### Step 8: 단건 조회에도 적용 (선택적)

`convertToGameData()` (line 258-326)와 `getTimelineData()` (line 86-94)에도 동일하게 적용 가능. 단건 조회용 `selectParticipantFramesByMatchId(String matchId)` 메서드를 별도 추가할 수도 있고, `selectParticipantFramesByMatchIds(List.of(matchId))`로 재사용할 수도 있음.

## 수정 대상 파일 요약

| 파일 | 변경 내용 |
|------|----------|
| `repository/match/dto/ParticipantFrameDTO.java` | **신규** — QueryProjection DTO |
| `repository/match/timeline/TimelineRepositoryCustom.java` | 메서드 시그니처 추가 |
| `repository/match/timeline/TimelineRepositoryCustomImpl.java` | QueryDSL 쿼리 구현 |
| `domain/match/domain/gamedata/timeline/ParticipantFrameData.java` | **신규** — 도메인 데이터 record |
| `domain/match/domain/gamedata/ParticipantData.java` | `frames` 필드 추가 |
| `repository/match/mapper/MatchMapper.java` | DTO→Domain 매핑 추가 |
| `repository/match/adapter/MatchPersistenceAdapter.java` | CompletableFuture 통합, assembleGameDataFromDTO 확장 |

## 검증

1. `./gradlew clean build` — QueryDSL Q클래스 생성 및 컴파일 확인
2. `TimelineRepositoryCustomImplTest`에 participant_frame 조회 테스트 추가
3. `MatchPersistenceAdapterTest`에 frames 통합 테스트 추가
4. `./gradlew test` — 전체 테스트 통과 확인