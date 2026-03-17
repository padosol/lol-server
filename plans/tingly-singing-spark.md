# DB 스키마 변경에 따른 엔티티 및 관련 로직 수정 계획

## Context

DB 서브모듈(`lol-db-schema`)이 통합 마이그레이션(`V1__init.sql`)으로 재구성되었습니다.
테이블 이름 변경, 컬럼 추가/삭제, 구조 변경(밴 분리, 타임라인 부모 테이블 제거, 룬 구조 개별 컬럼화)이 발생했으며,
현재 JPA 엔티티 및 관련 로직을 새 스키마에 맞게 수정해야 합니다.

**범위**: 엔티티 + persistence 계층 (Riot API 클라이언트 제외, 백업 테이블 엔티티 스킵)

---

## 1. MatchEntity 수정

**파일**: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/entity/MatchEntity.java`

### 변경사항
- `patchVersion` 필드 추가 (`patch_version` VARCHAR)

```java
@Column(name = "patch_version")
private String patchVersion;
```

---

## 2. MatchSummonerEntity → MatchParticipantEntity 리네임 + 수정

### 2-1. 엔티티 클래스 리네임 및 수정

**현재**: `repository/match/entity/MatchSummonerEntity.java`
**변경**: `repository/match/entity/MatchParticipantEntity.java`

| 항목 | 현재 | 변경 |
|------|------|------|
| 클래스명 | `MatchSummonerEntity` | `MatchParticipantEntity` |
| 테이블명 | `match_summoner` | `match_participant` |
| PK 컬럼 | `match_sumoner_id` | `match_participant_id` |
| Unique 제약 | `unique_index_match_id_and_puuid` | `unique_index_match_participant_puuid_match_id` |

### 2-2. 새 필드 추가

```java
private String summonerName;
private int basicPings;
private int dangerPings;
private int retreatPings;
private int damageDealtToEpicMonsters;
private int roleBoundItem;
private int playerAugment5;
private int playerAugment6;
```

### 2-3. StyleValue → PerkStyleValue 교체

**현재 `StyleValue`** (comma-separated string):
```java
private int primaryRuneId;
private String primaryRuneIds;  // "8100,8200,..."
private int secondaryRuneId;
private String secondaryRuneIds;
```

**새 `PerkStyleValue`** (개별 컬럼):
```java
@Embeddable
public class PerkStyleValue {
    private int primaryStyleId;
    private int primaryPerk0;
    private int primaryPerk1;
    private int primaryPerk2;
    private int primaryPerk3;
    private int subStyleId;
    private int subPerk0;
    private int subPerk1;
}
```

### 2-4. StatValue → PerkStatValue 컬럼명 변경

**현재 `StatValue`**: `defense`, `flex`, `offense` → 기본 매핑 `defense`, `flex`, `offense`
**새 `PerkStatValue`**: `@Column` 명시하여 `stat_perk_defense`, `stat_perk_flex`, `stat_perk_offense`

```java
@Embeddable
public class PerkStatValue {
    @Column(name = "stat_perk_defense")
    private int statPerkDefense;
    @Column(name = "stat_perk_flex")
    private int statPerkFlex;
    @Column(name = "stat_perk_offense")
    private int statPerkOffense;
}
```

### 2-5. Repository 리네임

| 현재 | 변경 |
|------|------|
| `MatchSummonerRepository` | `MatchParticipantRepository` |
| `MatchSummonerRepositoryCustom` | `MatchParticipantRepositoryCustom` |
| `MatchSummonerRepositoryCustomImpl` | `MatchParticipantRepositoryCustomImpl` |
| `MatchSummonerDTO` | `MatchParticipantDTO` |

**경로 변경**:
- `repository/match/matchsummoner/` → `repository/match/matchparticipant/`

### 2-6. MatchSummonerId 리네임

- `MatchSummonerId` → `MatchParticipantId` (composite ID)

---

## 3. ChallengesEntity → MatchParticipantChallengesEntity

**현재**: `repository/match/entity/ChallengesEntity.java`
**변경**: `repository/match/entity/MatchParticipantChallengesEntity.java`

| 항목 | 현재 | 변경 |
|------|------|------|
| 클래스명 | `ChallengesEntity` | `MatchParticipantChallengesEntity` |
| 테이블명 | `challenges` | `match_participant_challenges` |
| Unique 제약 | `unique_index_puuid_and_match_id` | `unique_index_match_participant_challenges_puuid_match_id` |

### 새 필드 추가

```java
private double earliestBaron;
private double healFromMapSources;
private double fastestLegendary;
private double shortestTimeToAceFromFirstTakedown;
```

---

## 4. MatchTeamEntity 수정

**파일**: `repository/match/entity/MatchTeamEntity.java`

### 4-1. 새 필드 추가

```java
// Atakhan objectives
private boolean atakhanFirst;
private int atakhanKills;

// Horde objectives
private boolean hordeFirst;
private int hordeKills;

// Feats
private int featEpicMonsterKill;
private int featFirstBlood;
private int featFirstTurret;
```

### 4-2. 밴 필드 제거 (MatchBanEntity로 분리)

아래 필드 삭제:
- `champion1Id`, `pick1Turn` ~ `champion5Id`, `pick5Turn`

`TeamBanValue.java` 도 삭제

---

## 5. 새 엔티티: MatchBanEntity 생성

**파일**: `repository/match/entity/MatchBanEntity.java`

```java
@Entity
@Table(name = "match_ban",
    uniqueConstraints = @UniqueConstraint(
        name = "unique_index_match_ban",
        columnNames = {"match_id", "team_id", "pick_turn"}))
public class MatchBanEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id")
    private String matchId;

    @Column(name = "team_id")
    private int teamId;

    @Column(name = "champion_id")
    private int championId;

    @Column(name = "pick_turn")
    private int pickTurn;
}
```

**새 Repository**: `MatchBanRepository` (JpaRepository)

---

## 6. 타임라인 엔티티 대규모 재구조화

### 6-1. TimeLineEventEntity 삭제

`time_line_event` 테이블이 새 스키마에 없으므로 삭제:
- `repository/match/entity/timeline/TimeLineEventEntity.java` 삭제
- 모든 이벤트 엔티티에서 `@ManyToOne TimeLineEventEntity` 제거, `matchId` 컬럼 직접 추가

### 6-2. 이벤트 엔티티별 변경

| 현재 클래스 | 현재 테이블 | 변경 클래스 | 변경 테이블 | 주요 변경 |
|------------|-----------|-----------|-----------|----------|
| `GameEventsEntity` | `game_events` | `GameEndEventEntity` | `game_end_event` | FK 제거, matchId 추가, type 제거 |
| `ItemEventsEntity` | `item_events` | `ItemEventEntity` | `item_event` | FK 제거, matchId 추가 |
| `SkillEventsEntity` | `skill_events` | `SkillLevelUpEventEntity` | `skill_level_up_event` | FK 제거, matchId 추가, type 제거 |
| `KillEventsEntity` | `kill_events` | `KillEventEntity` | `kill_event` | FK 제거, matchId 추가, type 제거, JSONB 필드 2개 추가 |
| `BuildingEventsEntity` | `building_events` | (유지) | (유지) | FK 제거, matchId 추가 |
| `WardEventsEntity` | `ward_events` | `WardEventEntity` | `ward_event` | FK 제거, matchId 추가, participantId→creatorId, type 제거 |
| `LevelEventsEntity` | `level_events` | `LevelUpEventEntity` | `level_up_event` | FK 제거, matchId 추가, type 제거 |
| `ChampionSpecialKillEventEntity` | (유지) | (유지) | (유지) | FK 제거, matchId 추가, type 제거 |
| `TurretPlateDestroyedEventEntity` | (유지) | (유지) | (유지) | FK 제거, matchId 추가 |

### 6-3. KillEventEntity 새 JSONB 필드

```java
@Column(name = "victim_damage_dealt", columnDefinition = "jsonb")
private String victimDamageDealt;

@Column(name = "victim_damage_received", columnDefinition = "jsonb")
private String victimDamageReceived;
```

### 6-4. 미사용 클래스 삭제

- `EventVictimDamageReceived.java` (이미 @Entity 주석처리됨)
- `EventVictimDamageDealt.java` (이미 @Entity 주석처리됨)
- `EventVictimDamageId.java`

---

## 7. MatchMapper 수정

**파일**: `repository/match/mapper/MatchMapper.java`

### 주요 변경

1. **클래스 참조 업데이트**: `MatchSummonerEntity` → `MatchParticipantEntity`, `ChallengesEntity` → `MatchParticipantChallengesEntity`

2. **PerkStyleValue → Style 매핑 로직 변경**:
   - 현재: comma-separated String → int[] 변환
   - 변경: 개별 필드 → int[] 조합
   ```java
   default Style mapPerkStyleToStyle(PerkStyleValue perk) {
       return Style.builder()
           .primaryRuneId(perk.getPrimaryStyleId())
           .primaryRuneIds(new int[]{perk.getPrimaryPerk0(), perk.getPrimaryPerk1(),
                                      perk.getPrimaryPerk2(), perk.getPrimaryPerk3()})
           .secondaryRuneId(perk.getSubStyleId())
           .secondaryRuneIds(new int[]{perk.getSubPerk0(), perk.getSubPerk1()})
           .build();
   }
   ```

3. **PerkStatValue → StatValue 매핑**: 필드명 매핑 (`statPerkDefense` → `defense` 등)

4. **TeamInfoData 매핑**: champion/pick 데이터를 `MatchBanEntity` 리스트에서 추출하도록 변경

5. **DTO 참조 업데이트**: `MatchSummonerDTO` → `MatchParticipantDTO`

---

## 8. QueryDSL Repository 수정

### 8-1. MatchParticipantRepositoryCustomImpl

- `QMatchSummonerEntity` → `QMatchParticipantEntity`
- `QChallengesEntity` → `QMatchParticipantChallengesEntity`
- `MatchSummonerDTO` → `MatchParticipantDTO`
- StyleValue/StatValue 프로젝션 → PerkStyleValue/PerkStatValue 프로젝션

### 8-2. MatchRepositoryCustomImpl

- MatchTeam 쿼리에서 ban 관련 프로젝션 제거
- `MatchTeamDTO`에서 champion/pick 필드 제거, atakhan/horde/feat 필드 추가

### 8-3. TimelineRepositoryCustom/Impl

- `TimeLineEventEntity` 참조 제거
- 이벤트 엔티티가 직접 `matchId`를 가지므로 조인 없이 직접 조회로 변경
- 리네임된 엔티티/Q-클래스 참조 업데이트

---

## 9. MatchPersistenceAdapter 수정

**파일**: `repository/match/adapter/MatchPersistenceAdapter.java`

- 클래스 참조 업데이트
- `MatchBanRepository` 의존성 추가
- ban 데이터 조회/조합 로직 추가 (기존: MatchTeamEntity에서 추출 → 변경: MatchBanEntity에서 조회)
- 타임라인 조회 로직 업데이트 (TimeLineEventEntity 제거에 따른 직접 조회)

---

## 10. 도메인 객체 (수정 최소화)

도메인 객체(`Style`, `TeamInfoData`, `StatValue` 등)는 **변경하지 않습니다**.
Persistence 계층의 Mapper에서 새 엔티티 구조 ↔ 기존 도메인 객체 간 변환을 처리합니다.

---

## 11. 테스트 업데이트

리네임된 클래스에 맞춰 테스트 파일 업데이트:
- `MatchMapperTest`
- `MatchPersistenceAdapterTest`
- `MatchRepositoryIntegrationTest`
- 기타 MatchSummoner/Challenges/Timeline 관련 테스트

---

## 12. 삭제 대상 파일

| 파일 | 이유 |
|------|------|
| `TimeLineEventEntity.java` | 테이블 삭제됨 |
| `EventVictimDamageReceived.java` | 미사용 (이미 주석처리) |
| `EventVictimDamageDealt.java` | 미사용 (이미 주석처리) |
| `EventVictimDamageId.java` | 미사용 |
| `TeamBanValue.java` | MatchBanEntity로 대체 |
| `StyleValue.java` | PerkStyleValue로 대체 |
| `StatValue.java` (persistence) | PerkStatValue로 대체 |
| `MatchSummonerId.java` | MatchParticipantId로 리네임 |

---

## 실행 순서

1. **Value Objects** 수정/생성 (PerkStyleValue, PerkStatValue)
2. **엔티티 수정** (MatchEntity, MatchParticipantEntity, MatchParticipantChallengesEntity, MatchTeamEntity, MatchBanEntity)
3. **타임라인 엔티티** 재구조화 (TimeLineEventEntity 삭제, 이벤트 엔티티 개별 수정)
4. **DTO 수정** (MatchParticipantDTO, MatchTeamDTO)
5. **Repository** 리네임 및 수정
6. **QueryDSL** 구현체 수정
7. **MatchMapper** 수정
8. **MatchPersistenceAdapter** 수정
9. **테스트** 업데이트
10. **미사용 파일 삭제**
11. **빌드 검증** (`./gradlew build`)

---

## 검증

```bash
# 빌드 성공 확인
./gradlew clean build

# 테스트 통과 확인
./gradlew test
```
