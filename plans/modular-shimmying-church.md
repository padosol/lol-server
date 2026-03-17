# 엔티티-마이그레이션 동기화 계획

## Context

V1__init.sql 마이그레이션 파일이 최신 DB 스키마를 정의하고 있으나, 현재 JPA 엔티티가 이 스키마와 불일치합니다. 엔티티를 마이그레이션에 맞춰 수정하고, 컴파일 오류가 발생하는 관련 코드(매퍼, 어댑터, 리포지토리, DTO)도 함께 수정합니다.

---

## Phase 1: 엔티티 수정

### 1. MatchSummonerEntity → `match_participant` (대규모)

**파일:** `.../repository/match/entity/MatchSummonerEntity.java`

#### a) 테이블/PK 변경
- `@Table(name = "match_summoner")` → `@Table(name = "match_participant")`
- PK `@Column(name = "match_sumoner_id")` → `@Column(name = "match_participant_id")`
- Unique constraint명: → `unique_index_match_participant_puuid_match_id`

#### b) 누락 필드 추가
- `summonerName` (String)
- `basicPings`, `dangerPings`, `retreatPings` (int)
- `damageDealtToEpicMonsters`, `roleBoundItem` (int)
- `playerAugment5`, `playerAugment6` (int)

#### c) Perk/Stat 임베디드 교체
- `@Embedded StyleValue styleValue` 제거 → `@Embedded PerkStyleValue perkStyle` (기존 클래스 활용)
- `@Embedded StatValue statValue` 제거 → `@Embedded PerkStatValue perkStat` (신규 생성)

#### d) @Column 어노테이션 추가
- `summoner1Id` → `@Column(name = "summoner1id")`
- `summoner2Id` → `@Column(name = "summoner2id")`
- `summoner1Casts` → `@Column(name = "summoner1casts")`
- `summoner2Casts` → `@Column(name = "summoner2casts")`

---

### 2. MatchEntity - `patch_version` 추가 (소규모)

**파일:** `.../repository/match/entity/MatchEntity.java`

- `private String patchVersion;` 추가

---

### 3. MatchTeamEntity - 구조 변경 (중규모)

**파일:** `.../repository/match/entity/MatchTeamEntity.java`

- **제거:** `champion1Id`~`champion5Id`, `pick1Turn`~`pick5Turn` (10개 필드, match_ban으로 분리)
- **추가:** `atakhanFirst`(boolean), `atakhanKills`(int), `hordeFirst`(boolean), `hordeKills`(int), `featEpicMonsterKill`(int), `featFirstBlood`(int), `featFirstTurret`(int)

---

### 4. MatchBanEntity 신규 생성

**파일:** `.../repository/match/entity/MatchBanEntity.java`

- Table: `match_ban`, Unique: `(match_id, team_id, pick_turn)`
- 필드: `id`, `matchId`, `teamId`, `championId`, `pickTurn`

---

### 5. ChallengesEntity - 테이블명/필드 변경 (중규모)

**파일:** `.../repository/match/entity/ChallengesEntity.java`

- `@Table(name = "challenges")` → `@Table(name = "match_participant_challenges")`
- Unique constraint명 → `unique_index_match_participant_challenges_puuid_match_id`
- **추가:** `earliestBaron`(double), `healFromMapSources`(double), `fastestLegendary`(double), `shortestTimeToAceFromFirstTakedown`(double)

---

### 6. Timeline 이벤트 - 구조 전면 변경 (대규모)

#### a) TimeLineEventEntity 삭제
- `.../repository/match/entity/timeline/TimeLineEventEntity.java` 삭제
- 마이그레이션에 `time_line_event` 테이블 없음

#### b) 모든 이벤트 엔티티 공통 변경
- `@ManyToOne TimeLineEventEntity` + `@JoinColumn` 제거
- `@Column(name = "match_id") private String matchId;` 추가
- PK `@Column` 어노테이션 제거 (기본 `id` 사용)

#### c) 테이블명 변경

| 엔티티 | 현재 | 마이그레이션 |
|--------|------|------------|
| ItemEventsEntity | `item_events` | `item_event` |
| SkillEventsEntity | `skill_events` | `skill_level_up_event` |
| KillEventsEntity | `kill_events` | `kill_event` |
| GameEventsEntity | `game_events` | `game_end_event` |
| LevelEventsEntity | `level_events` | `level_up_event` |
| WardEventsEntity | `ward_events` | `ward_event` |

#### d) 개별 변경
- **KillEventsEntity:** `victimDamageDealt`(JSONB), `victimDamageReceived`(JSONB) 추가
- **WardEventsEntity:** `participantId` → `creatorId`, `type` 제거

---

### 7. PerkStatValue 신규 생성

**파일:** `.../repository/match/entity/value/matchsummoner/PerkStatValue.java`

```java
@Embeddable
public class PerkStatValue {
    @Column(name = "stat_perk_defense") private int statPerkDefense;
    @Column(name = "stat_perk_flex") private int statPerkFlex;
    @Column(name = "stat_perk_offense") private int statPerkOffense;
}
```

### 8. 삭제 대상
- `StyleValue.java` → `PerkStyleValue`로 대체
- `StatValue.java` → `PerkStatValue`로 대체
- `PerkStatValue.java.tmp.*` 임시 파일 삭제

---

## Phase 2: 관련 코드 컴파일 오류 수정

엔티티 변경 후 컴파일 오류가 발생하는 파일들을 수정합니다.

### 매퍼
- **MatchMapper** (`.../mapper/MatchMapper.java`)
  - `StyleValue` → `PerkStyleValue` 매핑 변경 (string 파싱 → 개별 필드)
  - `StatValue` → `PerkStatValue` 매핑 변경
  - `mapChampionIds()`, `mapPickTurns()` 메서드 제거 (ban 분리)
  - 도메인 `Style` 클래스 매핑 업데이트

### DTO
- **MatchSummonerDTO** (`.../repository/match/dto/MatchSummonerDTO.java`)
  - `StyleValue` → `PerkStyleValue`, `StatValue` → `PerkStatValue`
  - 신규 필드 추가
- **MatchTeamDTO** (`.../repository/match/dto/MatchTeamDTO.java`)
  - champion/pick 필드 제거, atakhan/horde/feat 필드 추가
- **ItemEventDTO**, **SkillEventDTO** - timeLineEventEntity 참조 제거

### 리포지토리
- **TimelineRepositoryCustomImpl** (`.../match/timeline/TimelineRepositoryCustomImpl.java`)
  - `QTimeLineEventEntity` 참조 제거
  - fetchJoin 제거, `matchId` 직접 조건으로 변경
- **MatchSummonerRepositoryCustomImpl** - Q-타입 경로 업데이트

### 어댑터
- **MatchPersistenceAdapter** (`.../match/adapter/MatchPersistenceAdapter.java`)
  - TeamInfoData 매핑에서 ban 데이터 분리 처리
  - Timeline 이벤트 조회 로직 업데이트

### 도메인 (core 모듈)
- **Style.java** (`.../domain/match/domain/gamedata/value/Style.java`)
  - `int[]` 배열 방식에서 개별 필드 방식으로 변경 검토

---

## 검증

```bash
# 1. 빌드 확인
./gradlew clean build

# 2. 테스트 실행
./gradlew test

# 3. 컴파일 오류 발생 시 build-validator 스킬로 반복 수정
```
