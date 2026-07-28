# 타임라인 이벤트 읽기 경로: 9개 정규화 테이블 → `timeline_event_frame` (JSONB) 전환

## Context

lol-repository(`feature/timeline-event-jsonb`)에서 타임라인 이벤트 **쓰기** 경로가 9개 정규화 테이블(`item_event`, `skill_level_up_event`, `kill_event`, `building_events`, `ward_event`, `game_end_event`, `level_up_event`, `champion_special_kill_event`, `turret_plate_destroyed_event`)에서 단일 JSONB 테이블 `timeline_event_frame`(lol-db-schema V19)으로 통합됐다. 9개 테이블은 롤백 여지를 위해 구조만 남아 있고 **새 데이터는 더 이상 저장되지 않는다**.

따라서 lol-server가 지금 쓰고 있는 타임라인 조회 쿼리(`item_event` + `skill_level_up_event` UNION ALL)는 새로 적재되는 매치부터 **빈 결과**를 반환하게 된다. 타임라인 API(`GET /match/timeline/{matchId}`)의 연속성을 지키려면 읽기 경로를 `timeline_event_frame` 기반으로 갈아끼워야 한다.

> `participant_frame` 쓰기/스키마는 변경 없음 → 관련 조회(`MatchPersistenceAdapter.getMatchesBatch()`에서 쓰이는 participant/gold 타임라인)는 건드리지 않는다.

## 현재 읽기 경로 (파악 결과)

```
MatchController.getTimeline(matchId)
  └─ MatchService.getTimelineData(matchId)
       └─ MatchPersistenceAdapter.getTimelineData(matchId)
            └─ TimelineRepositoryCustomImpl.selectAllTimelineEventsByMatch(matchId)   ← 여기만 변경
                  → UNION ALL native SQL → List<TimelineEventDTO>
            ← MatchMapper.toItemEventsFromTimelineDTO / toSkillEventsFromTimelineDTO
            ← TimelineData.of(events)
```

현재 실제로 읽히는 이벤트는 **`ITEM_*` 4종 + `SKILL_LEVEL_UP` 1종뿐**이다. 나머지 7개 테이블(kill/building/ward/game_end/level_up/champion_special_kill/turret_plate)은 Entity 정의만 있고 읽기 경로에서 호출되지 않는다. 이번 작업 범위도 실제로 읽히는 2종에 한정한다. 나머지 타입 노출은 추후 별도 작업.

## 변경 파일 (최소 범위)

| 상태 | 경로 | 내용 |
|------|------|------|
| **수정** | `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/timeline/TimelineRepositoryCustomImpl.java` | 두 메서드의 native SQL을 `timeline_event_frame` JSONB 조회로 교체 |
| **신규** | `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/match/entity/timeline/TimelineEventFrameEntity.java` | 복합키(matchId, timestamp, eventIndex) + `data jsonb` 엔티티 |
| **유지** | `TimelineEventDTO` / `MatchMapper` / `MatchPersistenceAdapter` / `MatchService` / `MatchController` / `TimelineData` / 9개 기존 Entity | 인터페이스/응답 구조 불변. 9개 정규화 Entity는 롤백을 위해 유지 |
| **확인** | `lol-db-schema` submodule gitlink | V19 마이그레이션이 반영된 커밋을 가리키는지 확인 (코드 변경 없음) |

## 핵심 설계

### 1. `TimelineEventFrameEntity`

- 이 저장소의 JSONB 관행을 따라 **`String` 컬럼 + `@Column(columnDefinition = "jsonb")`** 조합을 쓴다 (`KillEventsEntity.victimDamageDealt`가 이미 동일 패턴). `hypersistence-utils`·`@JdbcTypeCode` 도입은 불필요 — 어차피 쿼리에서 JSONB 연산자로 필드를 꺼내고 엔티티 매핑으로는 읽지 않는다.
- 복합키는 `@IdClass`를 써서 `(matchId, timestamp, eventIndex)`로 구성. 이번 작업에서 엔티티 기반 쿼리는 쓰지 않지만, 나중에 타 타입을 노출할 때 재활용 가능.
- 아래 native SQL만 쓰면 Entity 없이도 동작하지만, 레퍼런스성으로 Entity를 만들어 두는 편이 이후 확장(예: Spring Data 레포 기반 조회)을 쉽게 한다.

### 2. `TimelineRepositoryCustomImpl` 쿼리 교체

`TimelineEventDTO`(6필드: matchId, participantId, eventId, eventType, timestamp, eventSource)를 **그대로 반환**하도록, Postgres JSON 연산자로 SQL 안에서 필드를 추출한다. → 상위 매퍼/도메인/컨트롤러 코드는 **1바이트도 안 바꿔도 된다**.

```sql
SELECT
    match_id,
    (data->>'participantId')::int AS participant_id,
    CASE data->>'type'
        WHEN 'SKILL_LEVEL_UP' THEN (data->>'skillSlot')::int
        ELSE                      (data->>'itemId')::int
    END AS event_id,
    CASE data->>'type'
        WHEN 'SKILL_LEVEL_UP' THEN data->>'levelUpType'
        ELSE                      data->>'type'
    END AS event_type,
    timestamp,
    CASE data->>'type'
        WHEN 'SKILL_LEVEL_UP' THEN 'SKILL'
        ELSE                      'ITEM'
    END AS event_source
FROM timeline_event_frame
WHERE match_id = :matchId
  AND data->>'type' IN ('ITEM_PURCHASED','ITEM_SOLD','ITEM_DESTROYED','ITEM_UNDO','SKILL_LEVEL_UP')
ORDER BY timestamp, event_index
```

배치 버전은 `match_id IN (:matchIds)`로 바꾼 동일 쿼리. 기존 `toTimelineEventDTO(Object[])` 매핑 로직은 변경 없이 재사용(컬럼 수·타입 동일).

### 3. 타입 맵 상수화

매직 스트링이 생기므로 `TimelineEventDTO` 옆에 `TimelineEventType` enum 또는 상수를 둬서 쿼리에서 참조하는 편이 안전하다. 단, 현재 쿼리는 native SQL 안에 들어가므로 enum을 쓰려면 SQL을 Java 문자열로 조립하거나 `:itemTypes` 바인딩 파라미터로 전달해야 한다. 바인딩 방식을 추천한다:

```java
.setParameter("itemTypes", List.of("ITEM_PURCHASED","ITEM_SOLD","ITEM_DESTROYED","ITEM_UNDO"))
```

→ 쿼리는 `data->>'type' = 'SKILL_LEVEL_UP' OR data->>'type' IN (:itemTypes)` 형태.

### 4. 인덱스 / 성능

- V19 스키마에 `data`에 GIN 인덱스가 걸려 있다. GIN은 `@>`·`?` 같은 containment/key-exists에 강하고 `data->>'type' = '...'`에는 직접 기여하지 않는다.
- `(match_id, timestamp)`가 PK이므로 `WHERE match_id = :matchId` 필터는 PK 인덱스를 탄다. 반환 row가 매치당 수백 건 수준이면 `data->>'type'` 체크는 in-memory 필터로 충분.
- 배치 조회(`match_id IN (...)`)에서 부하가 커지면 `CREATE INDEX ... ON timeline_event_frame ((data->>'type'))`처럼 **표현식 btree 인덱스**를 lol-db-schema에 추가 요청. 이번 PR에서는 성능 측정을 먼저 하고 필요 시 후속 PR.

### 5. 유지되는 9개 정규화 Entity

롤백 여지 확보 목적이므로 삭제하지 않는다. 다만 `MatchMapper` 등에서 쓰이지 않는 변환 메서드(`toDomain(ItemEventsEntity)` 등)는 이번 PR 기준으로는 **호출자가 없다**. 이건 후속 클린업 PR 후보로 메모만 남긴다.

## 테스트 전략

**이슈**: `RepositoryTestBase`가 `@DataJpaTest + @AutoConfigureTestDatabase(replace = NONE) + @ActiveProfiles("test")` 구성이라 `test` 프로파일 DB 세팅에 의존한다. 이 저장소에서 timeline 관련 레포지토리 테스트가 **H2로 돌고 있다면** JSONB 연산자(`->>`)가 파싱되지 않아 전환 즉시 깨진다.

현 상태 확인 → 두 가지 중 택:

1. **해당 테스트만 PostgreSQL Testcontainer로 전환**: Timeline 관련 테스트 클래스에 `@Testcontainers + @Container PostgreSQLContainer` 적용. 기존 `@DataJpaTest`와 조합 가능.
2. **테스트 임시 제외**: `@Disabled` + 후속 이슈 등록. 비권장(회귀 리스크).

추천은 1번. 이 repo에 Testcontainers 세팅이 없으면 해당 테스트 모듈 `build.gradle`에 `testImplementation 'org.testcontainers:postgresql'` 추가.

## 검증

1. **컴파일**: `./gradlew :module:infra:persistence:postgresql:compileJava`
2. **전체 빌드**: `./gradlew build` (실패 테스트는 위 "테스트 전략" 처리)
3. **로컬 수동 스모크**:
   ```bash
   ./gradlew bootRun -Dspring.profiles.active=local
   # 다른 터미널
   curl -s 'http://localhost:8080/match/timeline/{실제 matchId}' | jq .
   ```
   - 기대: `TimelineData` 응답에서 `participants` 맵 각 엔트리의 `itemEvents` / `skillEvents` 리스트가 채워짐
   - 데이터베이스에 V19 스키마와 실제 `timeline_event_frame` row가 있는 matchId로 검증
4. **쿼리 플랜 확인 (옵션)**:
   ```sql
   EXPLAIN ANALYZE
   SELECT ... FROM timeline_event_frame WHERE match_id = '...' AND data->>'type' IN (...);
   ```
   기존 UNION ALL 대비 지연·I/O 비교. 심각하게 열화되면 표현식 btree 인덱스 후속 PR.
5. **API 문서**: `./gradlew :module:infra:api:asciidoctor` — 응답 스키마는 불변이라 문서 변화 없을 것이지만 RestDocs 테스트가 있으면 통과 확인.

## 참조

- 기존 UNION ALL 쿼리: `module/infra/persistence/postgresql/.../timeline/TimelineRepositoryCustomImpl.java:17-56`
- 기존 JSONB 컬럼 패턴: `.../entity/timeline/events/KillEventsEntity.java` (`victimDamageDealt` `@Column(columnDefinition = "jsonb")`)
- 유사 JSON 컨버터(필요 시 참고): `.../repository/duo/converter/MostChampionListConverter.java`
- `participant_frame` Entity(구조 참고, 이번 변경 대상 아님): `.../entity/timeline/ParticipantFrameEntity.java`
- 최근 UNION ALL 통합 작업 기록: `docs/simplify/2026-04-16-timeline-union-all.md`
