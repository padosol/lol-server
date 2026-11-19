# e스포츠 로스터 API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** V5 로스터 스키마를 생명주기 결정(팀 분리·팀 내 이동·비활동 사유·은퇴 권리)에 맞게 고치고, 그 위에 관리자 입력 API(마스터·계약 사건·정정)와 공개 조회 API 를 새 `esports` 컨텍스트 모듈로 연다.

**Architecture:** 쓰기는 인물 한 명의 계약·사건 묶음(`PersonTimeline`)이 효력 발생일 기준으로 규칙을 검사하고, 어댑터가 "조기 종료 UPDATE → flush → 새 계약 INSERT" 순서로 저장한다. 읽기는 V5 뷰를 `NamedParameterJdbcTemplate` 로 그대로 읽는다. 소환사 정보는 다루지 않고 puuid 만 돌려준다.

**Tech Stack:** Spring Boot 3.3, JDK 21, Spring Data JPA + MapStruct, NamedParameterJdbcTemplate, PostgreSQL 16, Flyway, Testcontainers 1.21.4, JUnit 5 + AssertJ + Mockito, Spring RestDocs, ArchUnit 1.3.0

**Spec:** `docs/superpowers/specs/2026-11-19-esports-roster-api-design.md`

## Global Constraints

- Gradle 은 JDK 21 로: `export JAVA_HOME=/Users/sanhgoon/.sdkman/candidates/java/21.0.5-tem` (기본 JDK 25 는 "Unsupported class file major version 69" 로 실패)
- Testcontainers 테스트는 Docker 가 켜져 있어야 실제로 돈다. `disabledWithoutDocker` 로 건너뛰면 **검증 안 됨**으로 본다(결과에 SKIPPED 가 없어야 한다)
- 패키지 베이스 `com.example.lolserver.esports`. 모듈 경로 `:module:domain:esports`
- esports 는 다른 컨텍스트(member·community·duo·championstats)에 의존하지 않는다. 의존은 `:module:common` 만
- domain·application 은 `jakarta.persistence`, `org.springframework.web`, `org.springframework.data.jpa`, `org.springframework.jdbc`, `com.querydsl` 에 의존하지 않는다
- 응답 `ResponseEntity<ApiResponse<T>>`, POST 201 / GET·PUT·PATCH 200 / DELETE 204, 오류는 `CoreException(ErrorType)`
- 공개 `GET /api/v1/esports/**` (인증 없음), 관리자 `/api/v1/admin/esports/**` (ADMIN). 옛 `/api/...` 별칭은 만들지 않는다
- 사건 API 의 `date` 는 효력 발생일. 규칙은 오늘이 아니라 `date` 기준. 과거·미래 날짜 모두 받는다
- 기간은 양 끝 포함. "그날 종료" = `actual_end_date = date`, "전날 종료" = `actual_end_date = date - 1`
- V5 는 머지 전이라 **V5 파일을 직접 고친다**(V6 를 만들지 않는다)
- 커밋 메시지 `<type>: <한글 설명>`, 이슈 키·트레일러(Co-Authored-By 등) 없음
- 공통 `CoreExceptionAdvice` 는 바꾸지 않는다

## Review Focus

1. **Hibernate flush 순서** — Hibernate 는 INSERT 를 UPDATE 보다 먼저 보낸다. 이적·이동·조기 연장에서 직전 계약 조기 종료(UPDATE)보다 새 계약(INSERT)이 먼저 나가면 `ex_contract_no_overlap` 에 걸려 정상 사건이 409 가 된다. → Task 6 `transfer_저장은_직전계약_UPDATE를_먼저_반영한다`
2. **만료 경계일** — 11-16 만료 선수가 11-17 에 다른 팀에 가면 이적이 아니라 FA 입단이다. 이적 API 는 거부(`NO_ACTIVE_CONTRACT`)하고, 재계약 API 는 11-17 시작을 받아야 한다. → Task 4 `transfer_만료다음날은_NO_ACTIVE_CONTRACT`, `renew_만료다음날_재계약`
3. **뷰의 CURRENT_DATE** — 뷰는 오늘 기준이다. 고정 날짜로 테스트 데이터를 만들면 날짜가 지나면서 테스트가 깨진다. → Task 2·8 픽스처는 모두 `CURRENT_DATE ± n` 으로 만든다
4. **이력 없는 인물** — 계약·사건이 없는 인물(크리에이터)이 검색·프로필·소환사 조회에서 FA 로 보이면 안 된다(상태 `null`). → Task 8 `searchPersons_이력없는인물은_q검색에만_상태없이_나온다`, `findPersonByPuuid_이력없는인물은_status_null`
5. **DB 제약 위반이 500** — 정정 API 는 사건 규칙을 거치지 않으므로 겹침이 DB 까지 간다. 지금은 처리기가 없어 500 이다. → Task 6 `saveContract_겹치면_CONTRACT_PERIOD_CONFLICT`, Task 10 `correctContract_겹치면_409`

---

## File Structure

```
module/app/application/
  src/main/resources/db/migration/app/V5__add_esports_roster.sql        (Task 1·2 수정)
  src/test/java/com/example/lolserver/config/AppSchemaMigrationTest.java (Task 1·6 수정)
  src/test/java/com/example/lolserver/config/EsportsRosterViewTest.java  (Task 2 신규)
  build.gradle                                                           (Task 3 수정)
settings.gradle                                                          (Task 3 수정)
module/domain/esports/                                                   (Task 3~10 신규)
  build.gradle
  src/main/java/com/example/lolserver/esports/
    domain/            League, Season, Team, TeamName, TeamNameHistory, Person, PersonSummoner,
                       Contract, CareerEvent, PersonTimeline
    domain/vo/         Role, Position, ContractType, EndReason, CareerEventType, HiatusReason,
                       PersonStatus, ContractStatus
    application/       EsportsMasterService, PersonManagementService, ContractEventService,
                       ContractCorrectionService, EsportsQueryService
    application/command/   *Command (Task 7·9·10)
    application/port/in/   EsportsMasterUseCase, PersonManagementUseCase, ContractEventUseCase,
                           ContractCorrectionUseCase, EsportsQueryUseCase
    application/port/out/  LeaguePersistencePort, TeamPersistencePort, PersonPersistencePort,
                           PersonTimelinePort, EsportsQueryPort
    application/model/readmodel/  (Task 8)
    application/model/resultmodel/ContractEventResultModel
    adapter/in/web/        EsportsLeagueController, EsportsTeamController, EsportsPersonController (공개)
    adapter/in/web/admin/  AdminLeagueController, AdminTeamController, AdminPersonController,
                           AdminContractEventController, AdminCorrectionController
    adapter/in/web/request, adapter/in/web/response
    adapter/out/persistence/entity, repository, mapper, adapter
    adapter/out/persistence/query/EsportsQueryAdapter (+ RowMapper 들)
    adapter/out/persistence/support/EsportsConstraintTranslator
  src/test/java/com/example/lolserver/esports/
    ArchitectureTest, EsportsPostgresTestBase, domain/*Test, adapter/**/*Test
module/common/src/main/java/com/example/lolserver/common/error/ErrorType.java  (Task 3 수정)
module/domain/member/.../config/SecurityConfig.java                              (Task 9 수정)
module/domain/member/.../config/SecurityConfigAuthorizationTest.java (test)      (Task 9 수정)
module/domain/member/.../adapter/in/web/response/MemberResponse.java             (Task 11 수정)
CLAUDE.md, module/app/application/CLAUDE.md                                      (Task 11 수정)
```

## 브랜치

- Task 1·2: `feat/esports-roster-schema` (PR #189). 끝나면 push.
- Task 3 시작 전: `git switch feat/esports-roster-api && git merge --no-edit feat/esports-roster-schema` (force-push 금지라 rebase 하지 않는다)
- Task 3~11: `feat/esports-roster-api`

---

### Task 1: V5 테이블·제약 수정

**Files:**
- Modify: `module/app/application/src/main/resources/db/migration/app/V5__add_esports_roster.sql`
- Test: `module/app/application/src/test/java/com/example/lolserver/config/AppSchemaMigrationTest.java`

**Interfaces:**
- Produces: `esports_team.parent_team_id`, 제약 `fk_team_parent`·`ck_team_parent_not_self`, `ck_contract_type` 에 `MOVE`, `ck_contract_end_reason` 에 `MOVED`, `esports_career_event.reason` + `ck_career_event_reason`

- [ ] **Step 1: 실패하는 테스트 작성** — `AppSchemaMigrationTest` 에 추가

```java
@DisplayName("e스포츠 로스터 — 팀 내 이동(MOVE/MOVED)·상위 팀·비활동 사유 제약")
@Test
void esportsRosterLifecycleConstraints() throws Exception {
    flyway("postgresql-local.yml").migrate();
    executeInApp("""
            INSERT INTO esports_team (team_id, team_code, name) VALUES ('t1', 'T1', 'T1');
            INSERT INTO esports_team (team_id, team_code, name, parent_team_id) VALUES ('t1a', 'T1A', 'T1 Esports Academy', 't1');
            INSERT INTO person (person_id, nick_name) VALUES ('p1', 'Sol');
            INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date, actual_end_date, end_reason)
            VALUES ('p1', 't1a', 'PLAYER', 'MID', 'SIGN', '2026-01-01', '2026-11-16', '2026-05-31', 'MOVED');
            INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date)
            VALUES ('p1', 't1', 'PLAYER', 'MID', 'MOVE', '2026-06-01', '2026-11-16');
            INSERT INTO esports_career_event (person_id, event_type, event_date, reason) VALUES ('p1', 'HIATUS', '2026-07-01', 'MILITARY');
            """);

    assertThatThrownBy(() -> executeInApp("UPDATE esports_team SET parent_team_id = 't1' WHERE team_id = 't1';"))
            .isInstanceOf(SQLException.class).hasMessageContaining("ck_team_parent_not_self");
    assertThatThrownBy(() -> executeInApp("UPDATE esports_team SET parent_team_id = 'nope' WHERE team_id = 't1a';"))
            .isInstanceOf(SQLException.class).hasMessageContaining("fk_team_parent");
    assertThatThrownBy(() -> executeInApp(
            "INSERT INTO esports_career_event (person_id, event_type, event_date, reason) VALUES ('p1', 'RETIRE', '2026-08-01', 'REST');"))
            .isInstanceOf(SQLException.class).hasMessageContaining("ck_career_event_reason");
    assertThatThrownBy(() -> executeInApp(
            "INSERT INTO esports_career_event (person_id, event_type, event_date, reason) VALUES ('p1', 'HIATUS', '2026-09-01', 'VACATION');"))
            .isInstanceOf(SQLException.class).hasMessageContaining("ck_career_event_reason");
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :module:app:application:test --tests '*AppSchemaMigrationTest.esportsRosterLifecycleConstraints'`
Expected: FAIL — `column "parent_team_id" ... does not exist`

- [ ] **Step 3: V5 수정**
  - `esports_team` 에 `parent_team_id VARCHAR(30)` + `CONSTRAINT fk_team_parent FOREIGN KEY (parent_team_id) REFERENCES esports_team (team_id)` + `CONSTRAINT ck_team_parent_not_self CHECK (parent_team_id IS NULL OR parent_team_id <> team_id)` + `COMMENT ON COLUMN ... IS '같은 조직의 상위 팀 (CL 팀 → LCK 팀). 1단계만'`
  - `ck_contract_type` 값에 `'MOVE'`, `ck_contract_end_reason` 값에 `'MOVED'`. 4절 머리 주석의 contract_type·end_reason 목록에 `MOVE 팀 내 이동 (콜업·샌드다운·역할·포지션 변경. 직전 계약은 MOVED 로 끝나고 end_date 를 물려받는다)` / `MOVED 팀 내 이동` 추가
  - `esports_career_event` 에 `reason VARCHAR(20)` + `CONSTRAINT ck_career_event_reason CHECK (reason IS NULL OR (event_type = 'HIATUS' AND reason IN ('REST', 'SUSPENSION', 'MILITARY', 'PERSONAL')))` + 컬럼 주석
  - 머리 주석 예시 `(~2026-11-17 / 2026-11-18~)` → `(~2026-11-16 / 2026-11-17~)`

- [ ] **Step 4: 통과 확인** — `./gradlew :module:app:application:test --tests '*AppSchemaMigrationTest'` → 전부 PASS (SKIPPED 없음)

- [ ] **Step 5: 커밋** — `git commit -m "feat: e스포츠 로스터에 상위 팀·팀 내 이동·비활동 사유 제약을 추가한다"`

---

### Task 2: V5 유도 뷰 수정

**Files:**
- Modify: `module/app/application/src/main/resources/db/migration/app/V5__add_esports_roster.sql` (6·7절)
- Create: `module/app/application/src/test/java/com/example/lolserver/config/EsportsRosterViewTest.java`

**Interfaces:**
- Consumes: Task 1 의 컬럼·제약
- Produces (뷰 컬럼 — Task 8 이 읽는다):
  - `v_esports_contract_status`: V5 컬럼 + `organization_id` (`COALESCE(t.parent_team_id, t.team_id)`)
  - `v_esports_current_roster`: `SELECT s.*` 그대로 (organization_id 가 따라온다)
  - `v_esports_tenure`: `person_id, organization_id, as_player, tenure_no, start_date, last_day, end_reason, contract_count, renewal_count, loan, roles, team_ids`
  - `v_esports_person_status`: `person_id, nick_name, status, team_id, contract_team_id, role, position, contract_last_day, inactive, inactive_reason, retired_as_player, next_team_id, next_start_date, rights_team_id, rights_until`
  - `v_esports_roster_invalid`: `problem, person_id, contract_id, team_id, at_date` (문제 유형 추가)

- [ ] **Step 1: 실패하는 테스트 작성** — `EsportsRosterViewTest`. 컨테이너·Flyway 준비는 `AppSchemaMigrationTest` 와 같은 방식(`@Testcontainers(disabledWithoutDocker = true)`, `postgres:16-alpine`, `postgresql-local.yml` 의 flyway 설정). `@BeforeAll` 에서 한 번 migrate 하고 아래 픽스처를 넣는다. 모든 날짜는 `CURRENT_DATE ± n`.

  픽스처 (`d(n)` = `CURRENT_DATE + n`):
  - 팀: `t1`(T1), `t1a`(parent t1), `gen`(GEN), `gena`(parent gen), `x`(parent t1a — 2단계 중첩)
  - `p_move`: SIGN t1a [d(-400), d(200)] actual d(-101) MOVED → MOVE t1 [d(-100), d(200)]
  - `p_up`: SIGN t1a [d(-800), d(-366)] → RENEWAL t1 [d(-365), d(100)]
  - `p_loan`: SIGN gen [d(-300), d(300)] + LOAN t1 [d(-10), d(50)]
  - `p_ret`: SIGN gen [d(-500), d(200)] actual d(-30) RETIRED + RETIRE 사건 d(-30)
  - `p_ret2`: SIGN gena [d(-500), d(200)] actual d(-60) RETIRED + RETIRE 사건 d(-60) + UNRETIRE d(-20) + SIGN t1 [d(-10), d(300)]
  - `p_hiatus`: SIGN t1 [d(-100), d(100)] + HIATUS d(-5) reason MILITARY
  - `p_bad`: SIGN t1a [d(-200), d(100)] actual d(-51) RELEASED → MOVE t1 [d(-50), d(150)]
  - `p_coach_swap`: SIGN t1a PLAYER [d(-200), d(100)] actual d(-41) MOVED → MOVE t1 COACH [d(-40), d(100)]
  - `p_creator`: 인물만

  테스트(이름 · 단언):
  - `contract_status_같은조직_다음날_재계약이면_RENEWED` — p_up 첫 계약 `end_reason = 'RENEWED'`, `organization_id = 't1'`
  - `tenure_팀내이동과_승격은_조직단위_한줄` — p_move·p_up 각각 tenure 1행, `organization_id = 't1'`, `team_ids` 가 `{t1, t1a}` (순서 무관)
  - `person_status_임대중` — p_loan `status = 'ON_LOAN'`, `team_id = 't1'`, `contract_team_id = 'gen'`
  - `current_roster_임대선수는_임차팀에만` — `v_esports_current_roster` 에 p_loan 은 team_id t1 한 행
  - `person_status_계약중은퇴_권리` — p_ret `status = 'RETIRED'`, `rights_team_id = 'gen'`, `rights_until = d(200)`
  - `person_status_비활동` — p_hiatus `status = 'CONTRACTED'`, `inactive = true`, `inactive_reason = 'MILITARY'`
  - `person_status_이력없는인물은_없다` — p_creator 행 없음
  - `roster_invalid_새검사` — 다음 (problem, person_id/team_id) 가 모두 있다: `MOVE_NOT_CONTINUOUS`/p_bad, `MOVE_END_DATE_CHANGED`/p_bad, `MOVE_PLAYER_STAFF_SWITCH`/p_coach_swap, `SIGN_DURING_RETIRE_RIGHTS`/p_ret2, `TEAM_NESTED_TOO_DEEP`/team x. p_move·p_up 은 어떤 문제에도 없다

- [ ] **Step 2: 실패 확인** — `./gradlew :module:app:application:test --tests '*EsportsRosterViewTest'` → FAIL (`organization_id` 없음 등)

- [ ] **Step 3: 뷰 수정** (V5 6·7절)
  - `v_esports_contract_status`: `organization_id` 컬럼 추가. RENEWED 유도의 `n.team_id = c.team_id` 를 조직 비교로:
    ```sql
    WHEN c.end_date < CURRENT_DATE AND EXISTS (
         SELECT 1 FROM esports_contract n JOIN esports_team nt ON nt.team_id = n.team_id
          WHERE n.person_id = c.person_id
            AND COALESCE(nt.parent_team_id, nt.team_id) = COALESCE(t.parent_team_id, t.team_id)
            AND n.contract_type = 'RENEWAL'
            AND n.start_date = c.end_date + 1) THEN 'RENEWED'
    ```
  - `v_esports_tenure`: 파티션·그룹 키 `team_id` → `organization_id`. 출력의 `team_id` 대신 `organization_id`, `ARRAY_AGG(DISTINCT g.team_id) AS team_ids` 추가
  - `v_esports_person_status` 다시 쓰기: 진행 중 정식 계약 `mc`(status ACTIVE, LOAN 아님)와 진행 중 임대 `ln`(status ACTIVE, LOAN)을 `v_esports_contract_status` 에서 LEFT JOIN LATERAL 로 하나씩.
    - `status`: `ln` 있으면 `ON_LOAN`, `mc` 있으면 `CONTRACTED`, 마지막 RETIRE/UNRETIRE 가 RETIRE 면 `RETIRED`, 그 밖 `FREE_AGENT`
    - `team_id = COALESCE(ln.team_id, mc.team_id)`, `contract_team_id = mc.team_id`, `role`·`position` 도 `COALESCE(ln.*, mc.*)`, `contract_last_day = mc.last_day`
    - 사건 LATERAL(`event_date <= CURRENT_DATE`): `retired`(V5 와 같음), `on_hiatus`(V5 와 같음), `hiatus_reason = (ARRAY_AGG(e.reason ORDER BY e.event_date DESC, e.event_id DESC) FILTER (WHERE e.event_type IN ('HIATUS','RETURN')))[1]`
    - `inactive = COALESCE(on_hiatus, FALSE)`, `inactive_reason = CASE WHEN on_hiatus THEN hiatus_reason END`, `retired_as_player = COALESCE(retired, FALSE)`
    - `next_team_id`·`next_start_date`: V5 와 같음
    - 권리: `LEFT JOIN LATERAL (SELECT c.team_id, c.end_date FROM esports_contract c WHERE c.person_id = p.person_id AND c.end_reason = 'RETIRED' ORDER BY c.start_date DESC LIMIT 1) rr ON rr.end_date > CURRENT_DATE` → `rights_team_id`, `rights_until`
    - WHERE 는 V5 그대로(계약 또는 사건이 있는 인물만)
  - `v_esports_roster_invalid`: `seq` CTE 에 `esports_team` 을 JOIN 해 `org_id`, `LAG(org_id)`·`LAG(c.end_date)`·`LAG(c.role)` 추가. 검사:
    - `RENEWAL_NOT_CONTINUOUS`: `prev_org_id IS DISTINCT FROM org_id OR prev_last_day IS DISTINCT FROM start_date - 1`
    - `MOVE_NOT_CONTINUOUS`: MOVE 인데 `prev_end_reason IS DISTINCT FROM 'MOVED' OR prev_last_day IS DISTINCT FROM start_date - 1`
    - `MOVE_OUTSIDE_ORG`: MOVE 인데 `prev_org_id IS DISTINCT FROM org_id`
    - `MOVE_END_DATE_CHANGED`: MOVE 인데 `prev_end_date IS DISTINCT FROM end_date`
    - `MOVE_PLAYER_STAFF_SWITCH`: MOVE 인데 `(prev_role = 'PLAYER') IS DISTINCT FROM (role = 'PLAYER')`
    - `TEAM_NESTED_TOO_DEEP`: 부모의 `parent_team_id` 가 NULL 이 아닌 팀 (person_id·contract_id NULL, at_date NULL)
    - `SIGN_DURING_RETIRE_RIGHTS`: SIGN 계약 c 의 시작일이 같은 인물의 RETIRED 계약 r 의 `[actual_end_date + 1, end_date]` 안이고 두 계약의 조직이 다름
  - 뷰 COMMENT 도 갱신

- [ ] **Step 4: 통과 확인** — `./gradlew :module:app:application:test --tests '*EsportsRosterViewTest' --tests '*AppSchemaMigrationTest'` → PASS (SKIPPED 없음)

- [ ] **Step 5: 커밋 후 push** — `git commit -m "feat: e스포츠 로스터 뷰를 조직 단위·임대·은퇴 권리 기준으로 고친다"` → `git push` (PR #189 갱신)

---

### Task 3: esports 모듈 골격 · 값 타입 · 마스터 도메인 · 오류 유형

**Files:**
- Modify: `settings.gradle` (include `"module:domain:esports"`), `module/app/application/build.gradle` (`implementation project(":module:domain:esports")`)
- Create: `module/domain/esports/build.gradle`
- Create: `domain/vo/*.java`, `domain/League.java`, `domain/Season.java`, `domain/Team.java`, `domain/TeamName.java`, `domain/TeamNameHistory.java`, `domain/Person.java`, `domain/PersonSummoner.java`
- Modify: `module/common/src/main/java/com/example/lolserver/common/error/ErrorType.java`
- Test: `src/test/java/com/example/lolserver/esports/ArchitectureTest.java`, `domain/TeamTest.java`

**Interfaces:**
- Produces:
  - enums: `Role {PLAYER, HEAD_COACH, COACH; boolean isStaff()}`, `Position {TOP, JGL, MID, AD, SPT}`, `ContractType {SIGN, TRANSFER, RENEWAL, LOAN, MOVE}`, `EndReason {EXPIRED, RENEWED, TRANSFERRED, RELEASED, RETIRED, RECALLED, MOVED}` (EXPIRED 는 저장하지 않는다 — 읽기 전용), `CareerEventType {RETIRE, UNRETIRE, HIATUS, RETURN}`, `HiatusReason {REST, SUSPENSION, MILITARY, PERSONAL}`, `PersonStatus {CONTRACTED, ON_LOAN, FREE_AGENT, RETIRED}`, `ContractStatus {UPCOMING, ACTIVE, ENDED}`
  - 도메인 객체는 duo 처럼 `@Getter @Builder @AllArgsConstructor(access = PRIVATE)`, 필드는 V5 컬럼과 1:1 (타임스탬프 제외)
  - `record TeamName(String teamCode, String name, String nameEng, String imageUrl, String darkImageUrl)`
  - `Team`: `String organizationId()` (parentTeamId ?: teamId), `void validateParent(Team parent)`, `TeamNameHistory rename(TeamName newName, LocalDate renamedOn, LocalDate previousNameFrom, String note)`, `void correctCurrentName(TeamName name, LocalDate nameSince)`, `void disband(LocalDate disbandedAt)`, `void update(String homeLeagueId, String parentTeamId, LocalDate foundedAt)`
  - `ErrorType` 상수 (스펙 7.5 그대로): 404 `ESPORTS_LEAGUE_NOT_FOUND, ESPORTS_SEASON_NOT_FOUND, ESPORTS_TEAM_NOT_FOUND, PERSON_NOT_FOUND, CONTRACT_NOT_FOUND, CAREER_EVENT_NOT_FOUND, TEAM_NAME_HISTORY_NOT_FOUND`; 409 `ESPORTS_ID_DUPLICATED, SUMMONER_ALREADY_LINKED, CONTRACT_PERIOD_CONFLICT, PERSON_IN_USE, TEAM_HAS_ACTIVE_CONTRACT`; 400 `NO_ACTIVE_CONTRACT, NOT_A_CONTINUATION, SAME_ORGANIZATION, DIFFERENT_ORGANIZATION, NOTHING_CHANGED, PLAYER_STAFF_SWITCH, NOT_A_PLAYER, ALREADY_ON_LOAN, LOAN_EXCEEDS_CONTRACT, NOT_A_LOAN, PERSON_RETIRED, PERSON_NOT_RETIRED, ALREADY_INACTIVE, NOT_INACTIVE, TEAM_NESTED_TOO_DEEP`. 메시지는 한국어 한 문장(예: `NO_ACTIVE_CONTRACT` "효력 발생일에 진행 중인 계약이 없습니다.")

- [ ] **Step 1: `build.gradle`** — duo 를 본떠 `lolserver.web-conventions` + `lolserver.persistence-conventions`, `implementation project(':module:common')`, `testImplementation(testFixtures(project(':module:common')))`, `testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'`, Testcontainers(`ext['testcontainers.version'] = '1.21.4'`, `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter`), `testImplementation 'org.flywaydb:flyway-core'`, `testRuntimeOnly 'org.flywaydb:flyway-database-postgresql'`, `testRuntimeOnly 'org.postgresql:postgresql'`. `tasks.named('test') { systemProperty 'esports.appMigrationDir', rootProject.file('module/app/application/src/main/resources/db/migration/app').absolutePath }`

- [ ] **Step 2: 실패하는 테스트 작성**
  - `ArchitectureTest` — duo 의 규칙 4개(domain→application/adapter 금지, application→adapter 금지, domain·application→인프라 타입 금지(+`org.springframework.jdbc..`), adapter.in→adapter.out 금지) + `다른_컨텍스트에_의존하지_않는다` (`member`, `community`, `duo`, `championstats` 패키지)
  - `TeamTest`:
    - `organizationId_상위팀이_있으면_상위팀` — t1a(parent t1) → `"t1"`, t1 → `"t1"`
    - `validateParent_상위팀에_상위팀이_있으면_TEAM_NESTED_TOO_DEEP`
    - `validateParent_자기자신이면_TEAM_NESTED_TOO_DEEP`
    - `rename_현재이름을_이력으로_옮긴다` — nameSince 2020-01-01 인 팀을 2026-11-20 에 개명 → history `validFrom 2020-01-01, validTo 2026-11-19`, 옛 코드·이름, 팀은 새 이름·`nameSince 2026-11-20`
    - `rename_nameSince와_foundedAt이_없으면_previousNameFrom을_쓴다`, 셋 다 없으면 `INVALID_INPUT`
    - `rename_개명일이_validFrom이하면_INVALID_INPUT`

- [ ] **Step 3: 실패 확인** — `./gradlew :module:domain:esports:test` → 컴파일 실패

- [ ] **Step 4: 구현** — enums, 도메인 객체, `ErrorType` 상수, settings/app build 등록

- [ ] **Step 5: 통과 확인** — `./gradlew :module:domain:esports:test :module:app:application:compileJava` → PASS

- [ ] **Step 6: 커밋** — `git commit -m "feat: esports 모듈과 로스터 값 타입·마스터 도메인을 추가한다"`

---

### Task 4: 계약 타임라인 — 입단 · 재계약 · 이적 · 팀 내 이동

**Files:**
- Create: `domain/Contract.java`, `domain/CareerEvent.java`, `domain/PersonTimeline.java`
- Test: `domain/ContractTest.java`, `domain/PersonTimelineContractTest.java`

**Interfaces:**
- Consumes: Task 3 enums, `Team.organizationId()`, `ErrorType`
- Produces:
  - `Contract` 필드: `Long contractId, String personId, String teamId, String organizationId, Role role, Position position, ContractType contractType, LocalDate startDate, LocalDate endDate, LocalDate actualEndDate, EndReason endReason, String note` (`organizationId` 는 저장하지 않는 파생값 — 어댑터가 채운다)
  - `static Contract open(String personId, Team team, Role role, Position position, ContractType type, LocalDate startDate, LocalDate endDate, String note)` — PLAYER 는 position 필수, 스태프는 position 금지, `endDate < startDate` 금지 → `INVALID_INPUT`
  - `LocalDate lastDay()` (actualEndDate ?: endDate, null = 끝 없음), `boolean isEffectiveOn(LocalDate d)`, `boolean overlaps(LocalDate start, LocalDate last)` (null last = 무한), `boolean isLoan()`
  - `void endEarly(LocalDate lastDay, EndReason reason)` — 이미 `endReason` 있으면 `CONTRACT_PERIOD_CONFLICT`, `lastDay < startDate` 또는 `lastDay > endDate` 면 `NO_ACTIVE_CONTRACT`
  - `void correct(LocalDate startDate, LocalDate endDate, LocalDate actualEndDate, EndReason endReason, Position position, String note)` — Task 10 이 쓴다. `open` 과 같은 검사 + actualEndDate·endReason 짝 검사(`INVALID_INPUT`)
  - `CareerEvent` 필드: `Long eventId, String personId, CareerEventType eventType, LocalDate eventDate, String teamId, HiatusReason reason, String note, String sourceUrl`; `static CareerEvent of(String personId, CareerEventType type, LocalDate date, String teamId, HiatusReason reason, String note, String sourceUrl)`
  - `PersonTimeline(String personId, List<Contract> contracts, List<CareerEvent> events)`
    - `Contract sign(Team team, Role role, Position position, LocalDate date, LocalDate endDate, String note)`
    - `Contract renew(Team team, Role role, Position position, LocalDate date, LocalDate endDate, String note)`
    - `Contract transfer(Team team, Role role, Position position, LocalDate date, LocalDate endDate, String note)`
    - `Contract move(Team team, Role role, Position position, LocalDate date, String note)`
    - `List<Contract> endedContracts()` (이번에 조기 종료된 기존 계약 — 먼저 UPDATE), `List<Contract> newContracts()`, `List<CareerEvent> newEvents()`

  규칙(효력일 `d`, "정식 계약" = LOAN 아님):
  - 공통: 새 정식 계약 `[d, endDate]` 가 (조기 종료 반영 후) 다른 정식 계약과 겹치면 `CONTRACT_PERIOD_CONFLICT`
  - `sign`: PLAYER 이고 `d` 이하 마지막 RETIRE/UNRETIRE 가 RETIRE 면 `PERSON_RETIRED`
  - `renew`: `d-1` 에 유효한 정식 계약 `prev` 중 `prev.organizationId == team.organizationId()` 이고 `prev.endReason == null` 인 것이 없으면 `NOT_A_CONTINUATION`. 역할군(선수/스태프)이 바뀌면 `PLAYER_STAFF_SWITCH`. `prev.lastDay()` 가 null 이거나 `> d-1` 이면 `prev.endEarly(d-1, RENEWED)`. 새 계약 유형 RENEWAL
  - `transfer`: `d` 와 `d-1` 모두에 유효한 정식 계약 `prev` 가 없으면 `NO_ACTIVE_CONTRACT`. 같은 조직이면 `SAME_ORGANIZATION`. 역할군 변경 `PLAYER_STAFF_SWITCH`. `d` 에 유효한 임대가 있으면: 임대팀 조직 == 이적팀 조직이면 그 임대도 `endEarly(d-1, TRANSFERRED)`(구매 선택권), 아니면 `ALREADY_ON_LOAN`. `prev.endEarly(d-1, TRANSFERRED)`. 새 계약 유형 TRANSFER
  - `move`: `prev` 조건은 transfer 와 같음(`NO_ACTIVE_CONTRACT`). 다른 조직 `DIFFERENT_ORGANIZATION`. 팀·역할·포지션이 모두 같으면 `NOTHING_CHANGED`. 역할군 변경 `PLAYER_STAFF_SWITCH`. `d` 에 유효한 임대가 있으면 `ALREADY_ON_LOAN`. `prev.endEarly(d-1, MOVED)`, 새 MOVE 계약 `[d, prev.endDate]`

- [ ] **Step 1: 실패하는 테스트 작성**
  - `ContractTest`: `open_선수인데_포지션없으면_INVALID_INPUT`, `open_코치인데_포지션있으면_INVALID_INPUT`, `open_종료일이_시작일보다_빠르면_INVALID_INPUT`, `lastDay_조기종료일이_있으면_조기종료일`, `endEarly_이미_종료사유가_있으면_CONTRACT_PERIOD_CONFLICT`
  - `PersonTimelineContractTest` (팀 픽스처: t1, t1a(parent t1), gen):
    - `sign_FA_입단` — 계약 없음, sign(gen, PLAYER, MID, 2026-11-17, 2027-11-15) → newContracts 1개 SIGN
    - `sign_정식계약과_겹치면_CONTRACT_PERIOD_CONFLICT`
    - `sign_은퇴상태_선수계약은_PERSON_RETIRED`, `sign_은퇴상태라도_코치계약은_허용`
    - `sign_과거이력은_뒤기록과_겹치지않으면_끼워넣는다` — 2020 계약 있음, 2017-11-20~2018-11-18 sign → 성공
    - `renew_만료다음날_재계약은_직전계약을_건드리지않는다` — SIGN t1 [2024-11-18, 2026-11-16], renew(t1, PLAYER, MID, 2026-11-17, 2028-11-20) → endedContracts 비어 있음, 새 RENEWAL [2026-11-17, 2028-11-20]
    - `renew_만료전_연장은_직전계약을_RENEWED로_조기종료` — 같은 계약에 renew(…, 2026-03-01, 2029-11-19) → prev `actualEndDate 2026-02-28`, `endReason RENEWED`
    - `renew_공백뒤면_NOT_A_CONTINUATION` — renew date 2026-12-01
    - `renew_같은조직_다른팀으로_재계약하며_승격` — prev t1a, renew t1 2026-11-17 → 성공
    - `renew_다른조직이면_NOT_A_CONTINUATION`, `renew_선수에서_코치면_PLAYER_STAFF_SWITCH`
    - `transfer_직전계약을_전날_TRANSFERRED로_종료` — SIGN t1 [2025-11-18, 2027-11-15], transfer(gen, …, 2026-11-20, 2028-11-20) → prev `actualEndDate 2026-11-19, TRANSFERRED`, 새 TRANSFER 시작 2026-11-20
    - `transfer_만료다음날은_NO_ACTIVE_CONTRACT` — prev [.., 2026-11-16], date 2026-11-17
    - `transfer_같은조직이면_SAME_ORGANIZATION`
    - `transfer_임차팀으로_이적하면_임대도_TRANSFERRED` — 원 계약 t1 + LOAN gen 진행 중, transfer(gen) → 임대·원 계약 모두 `TRANSFERRED`
    - `transfer_다른팀에_임대중이면_ALREADY_ON_LOAN`
    - `transfer_조기종료가_이미_기록된_계약이면_CONTRACT_PERIOD_CONFLICT`
    - `move_콜업은_end_date를_물려받는다` — SIGN t1a [2026-01-01, 2026-11-16], move(t1, PLAYER, MID, 2026-06-01) → prev `2026-05-31 MOVED`, 새 MOVE [2026-06-01, 2026-11-16]
    - `move_같은팀_포지션전향은_허용`, `move_코치에서_감독은_허용`
    - `move_바뀐것이_없으면_NOTHING_CHANGED`, `move_다른조직이면_DIFFERENT_ORGANIZATION`, `move_선수에서_코치면_PLAYER_STAFF_SWITCH`, `move_임대중이면_ALREADY_ON_LOAN`

- [ ] **Step 2: 실패 확인** — `./gradlew :module:domain:esports:test --tests '*ContractTest' --tests '*PersonTimelineContractTest'` → 컴파일 실패

- [ ] **Step 3: 구현** — `Contract`, `CareerEvent`, `PersonTimeline`(sign·renew·transfer·move + 공통 겹침 검사 + 변경 목록)

- [ ] **Step 4: 통과 확인** — 위 명령 PASS

- [ ] **Step 5: 커밋** — `git commit -m "feat: 계약 타임라인에 입단·재계약·이적·팀 내 이동 규칙을 추가한다"`

---

### Task 5: 계약 타임라인 — 임대 · 임대 복귀 · 방출 · 은퇴 · 은퇴 번복 · 휴식 · 복귀

**Files:**
- Modify: `domain/PersonTimeline.java`
- Test: `domain/PersonTimelineCareerTest.java`

**Interfaces:**
- Consumes: Task 4 `Contract`, `CareerEvent`, `PersonTimeline`
- Produces (PersonTimeline):
  - `Contract loan(Team team, Position position, LocalDate date, LocalDate endDate, String note)` — `d` 에 유효한 정식 계약 없으면 `NO_ACTIVE_CONTRACT`; 그 계약이 PLAYER 아니면 `NOT_A_PLAYER`; 같은 조직 `SAME_ORGANIZATION`; `endDate` 가 원 계약 `lastDay()` 보다 뒤면 `LOAN_EXCEEDS_CONTRACT`; `[d, endDate]` 와 겹치는 임대가 있으면 `ALREADY_ON_LOAN`; position null 이면 원 계약 포지션; 역할 PLAYER, 유형 LOAN
  - `void recall(Long loanContractId, LocalDate date)` — LOAN 아니면 `NOT_A_LOAN`(없는 id 도), `date` 에 유효하지 않으면 `NO_ACTIVE_CONTRACT`; `endEarly(date, RECALLED)`
  - `void release(LocalDate date)` — `date` 에 유효한 정식 계약 없으면 `NO_ACTIVE_CONTRACT`; 정식 계약과 유효한 임대를 `endEarly(date, RELEASED)`
  - `CareerEvent retire(LocalDate date, String note, String sourceUrl)` — `date` 이하 마지막 RETIRE/UNRETIRE 가 RETIRE 면 `PERSON_RETIRED`; 유효한 정식 계약이 스태프면 `NOT_A_PLAYER`; 선수 계약·임대를 `endEarly(date, RETIRED)`; RETIRE 사건(teamId = 종료한 정식 계약 팀)
  - `CareerEvent unretire(LocalDate date, String note, String sourceUrl)` — 은퇴 상태 아니면 `PERSON_NOT_RETIRED`
  - `CareerEvent hiatus(LocalDate date, HiatusReason reason, String note, String sourceUrl)` — `date` 이하 마지막 HIATUS/RETURN 이 HIATUS 면 `ALREADY_INACTIVE`
  - `CareerEvent comeBack(LocalDate date, String note, String sourceUrl)` — 비활동 아니면 `NOT_INACTIVE` (`return` 은 예약어라 이름이 다르다)

- [ ] **Step 1: 실패하는 테스트 작성** — `PersonTimelineCareerTest`
  - `loan_원계약은_그대로_두고_LOAN을_만든다` — SIGN gen [2026-01-01, 2027-11-15], loan(t1, null, 2026-11-20, 2027-06-30) → endedContracts 비어 있음, 새 LOAN 포지션 = 원 계약 포지션
  - `loan_코치는_NOT_A_PLAYER`, `loan_원계약이_없으면_NO_ACTIVE_CONTRACT`, `loan_같은조직이면_SAME_ORGANIZATION`, `loan_원계약보다_길면_LOAN_EXCEEDS_CONTRACT`, `loan_이미_임대중이면_ALREADY_ON_LOAN`
  - `recall_그날_RECALLED로_종료`, `recall_임대가_아니면_NOT_A_LOAN`, `recall_이미_끝난_임대면_NO_ACTIVE_CONTRACT`
  - `release_정식계약과_임대를_그날_RELEASED로_종료`, `release_계약이_없으면_NO_ACTIVE_CONTRACT`
  - `retire_선수계약을_RETIRED로_끝내고_RETIRE사건을_남긴다` — 사건 teamId = 원 계약 팀
  - `retire_FA는_사건만_남긴다`, `retire_이미_은퇴면_PERSON_RETIRED`, `retire_스태프계약중이면_NOT_A_PLAYER`
  - `unretire_은퇴상태가_아니면_PERSON_NOT_RETIRED`, `unretire_후에는_선수로_입단할수있다`
  - `hiatus_사유를_남긴다`, `hiatus_이미_비활동이면_ALREADY_INACTIVE`, `comeBack_비활동이_아니면_NOT_INACTIVE`

- [ ] **Step 2: 실패 확인** — `./gradlew :module:domain:esports:test --tests '*PersonTimelineCareerTest'` → 컴파일 실패
- [ ] **Step 3: 구현** — 위 7개 메서드
- [ ] **Step 4: 통과 확인** — `./gradlew :module:domain:esports:test` → PASS
- [ ] **Step 5: 커밋** — `git commit -m "feat: 계약 타임라인에 임대·방출·은퇴·휴식 규칙을 추가한다"`

---

### Task 6: 영속화 — 엔티티 · 쓰기 어댑터 · 제약 위반 변환

**Files:**
- Create: `adapter/out/persistence/entity/` `EsportsLeagueEntity, EsportsSeasonEntity, EsportsTeamEntity, EsportsTeamNameHistoryEntity, PersonEntity, PersonSummonerEntity, EsportsContractEntity, EsportsCareerEventEntity`
- Create: `adapter/out/persistence/repository/*JpaRepository`, `adapter/out/persistence/mapper/*Mapper` (MapStruct, enum ↔ String)
- Create: `adapter/out/persistence/adapter/` `LeaguePersistenceAdapter, TeamPersistenceAdapter, PersonPersistenceAdapter, PersonTimelinePersistenceAdapter`
- Create: `adapter/out/persistence/support/EsportsConstraintTranslator.java`
- Create: `application/port/out/` `LeaguePersistencePort, TeamPersistencePort, PersonPersistencePort, PersonTimelinePort`
- Modify: `module/app/application/src/test/java/com/example/lolserver/config/AppSchemaMigrationTest.java` (`APP_ENTITY_PACKAGES` 에 `"com.example.lolserver.esports.adapter.out.persistence.entity"`)
- Test: `src/test/java/com/example/lolserver/esports/EsportsPostgresTestBase.java`, `adapter/out/persistence/*AdapterTest.java`

**Interfaces:**
- Consumes: Task 3~5 도메인
- Produces:
  - `LeaguePersistencePort`: `boolean existsLeague(String)`, `Optional<League> findLeague(String)`, `League saveLeague(League)`, `boolean existsSeason(String)`, `Optional<Season> findSeason(String)`, `Season saveSeason(Season)`
  - `TeamPersistencePort`: `boolean exists(String)`, `Optional<Team> findById(String)`, `Team save(Team)`, `TeamNameHistory saveNameHistory(TeamNameHistory)`, `Optional<TeamNameHistory> findNameHistory(String teamId, Long historyId)`, `void deleteNameHistory(Long historyId)`, `boolean hasContractEffectiveOnOrAfter(String teamId, LocalDate date)`
  - `PersonPersistencePort`: `boolean exists(String)`, `Optional<Person> findById(String)`, `Person save(Person)`, `void delete(String)`, `boolean hasRecords(String personId)` (계약·사건·소환사 중 하나라도), `Optional<PersonSummoner> findSummoner(String puuid)`, `List<PersonSummoner> findSummoners(String personId)`, `PersonSummoner saveSummoner(PersonSummoner)` (isMain 이면 같은 인물의 다른 대표를 **벌크 UPDATE 로 먼저 해제** 후 저장), `void deleteSummoner(String puuid)`
  - `PersonTimelinePort`: `PersonTimeline load(String personId)` (계약의 `organizationId` 를 팀 조회로 채운다), `PersonTimeline save(PersonTimeline)` (① `endedContracts` 를 `saveAll` + `flush` ② `newContracts` 저장 + flush ③ `newEvents` 저장 + flush, 생성 ID 를 채운 타임라인 반환), `Optional<Contract> findContract(Long)`, `Contract saveContract(Contract)`, `void deleteContract(Long)`, `Optional<CareerEvent> findEvent(Long)`, `void deleteEvent(Long)`
  - `EsportsConstraintTranslator.translate(DataIntegrityViolationException e): CoreException` — 원인 메시지의 제약 이름으로: `ex_contract_*` → `CONTRACT_PERIOD_CONFLICT`, `pk_*`·`uk_*` → `ESPORTS_ID_DUPLICATED`, `fk_*`·`ck_*`·그 밖 → `INVALID_INPUT`. 모든 쓰기 어댑터 메서드는 `saveAndFlush`/`flush` 후 `DataIntegrityViolationException` 을 이것으로 바꿔 던진다
  - 엔티티: `created_at`·`updated_at` 은 `@CreationTimestamp`/`@UpdateTimestamp`. 문자열 PK 엔티티의 생성은 서비스가 `exists` 로 중복을 먼저 막는다(Spring Data `save` 가 merge 로 덮어쓰기 때문)
  - `EsportsPostgresTestBase`: `@DataJpaTest @ContextConfiguration(classes = TestJpaConfig.class) @AutoConfigureTestDatabase(replace = NONE) @Import(QueryDslConfig.class) @Testcontainers(disabledWithoutDocker = true)`, `@Container static PostgreSQLContainer<?>("postgres:16-alpine")`, `@DynamicPropertySource` 로 `spring.datasource.url = jdbcUrl + "&currentSchema=app"`, username/password, `spring.flyway.enabled=true`, `spring.flyway.locations = "filesystem:" + System.getProperty("esports.appMigrationDir")`, `spring.flyway.schemas=app`, `spring.flyway.default-schema=app`, `spring.flyway.create-schemas=true`, `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect`. 테스트 헬퍼 `protected void sql(String script)` (`JdbcTemplate.execute`)

- [ ] **Step 1: 실패하는 테스트 작성**
  - `PersonTimelinePersistenceAdapterTest extends EsportsPostgresTestBase`
    - `load_계약의_organizationId를_상위팀으로_채운다` — t1a 계약 → `organizationId "t1"`
    - `transfer_저장은_직전계약_UPDATE를_먼저_반영한다` — SIGN t1 [2025-11-18, 2027-11-15] 저장 → load → `transfer(gen, …, 2026-11-20, 2028-11-20)` → `save` 가 예외 없이 끝나고 DB 에 2행, 직전 계약 `actual_end_date 2026-11-19`
    - `save_생성된_계약과_사건에_ID가_채워진다`
    - `saveContract_겹치면_CONTRACT_PERIOD_CONFLICT` — 도메인 검사 없이 겹치는 계약을 `saveContract` → `CoreException(CONTRACT_PERIOD_CONFLICT)`
  - `TeamPersistenceAdapterTest`: `save_후_findById_상위팀과_이름이_같다`, `saveNameHistory_기간이_겹치면_CONTRACT_PERIOD_CONFLICT`(ex_team_name_no_overlap), `hasContractEffectiveOnOrAfter_해체일이후_계약이_있으면_true`
  - `PersonPersistenceAdapterTest`: `saveSummoner_대표계정을_바꾸면_이전대표가_해제된다`, `hasRecords_계약이나_사건이나_소환사가_있으면_true`
  - `AppSchemaMigrationTest` 는 패키지 추가만으로 esports 엔티티를 validate 한다

- [ ] **Step 2: 실패 확인** — `./gradlew :module:domain:esports:test --tests '*PersistenceAdapterTest'` → 컴파일 실패
- [ ] **Step 3: 구현** — 엔티티·리포지토리·매퍼·어댑터·포트·번역기
- [ ] **Step 4: 통과 확인** — `./gradlew :module:domain:esports:test :module:app:application:test --tests '*AppSchemaMigrationTest'` → PASS (SKIPPED 없음)
- [ ] **Step 5: 커밋** — `git commit -m "feat: e스포츠 로스터 엔티티와 쓰기 어댑터를 추가한다"`

---

### Task 7: 마스터 데이터 관리자 API — 리그 · 시즌 · 팀 · 인물 · 소환사 연결

**Files:**
- Create: `application/port/in/EsportsMasterUseCase.java`, `application/port/in/PersonManagementUseCase.java`, `application/EsportsMasterService.java`, `application/PersonManagementService.java`, `application/command/*`
- Create: `adapter/in/web/admin/AdminLeagueController.java`, `AdminTeamController.java`, `AdminPersonController.java`, `adapter/in/web/request/*`, `adapter/in/web/response/*`
- Test: `application/EsportsMasterServiceTest.java`, `application/PersonManagementServiceTest.java`, `adapter/in/web/admin/Admin*ControllerTest.java` (RestDocs)

**Interfaces:**
- Consumes: Task 6 포트들
- Produces:
  - `EsportsMasterUseCase`: `League createLeague(LeagueCommand)`, `League updateLeague(String leagueId, LeagueCommand)`, `Season createSeason(String leagueId, SeasonCommand)`, `Season updateSeason(String seasonId, SeasonCommand)`, `Team createTeam(CreateTeamCommand)`, `Team updateTeam(String teamId, UpdateTeamCommand)`, `Team renameTeam(String teamId, RenameTeamCommand)`, `Team correctCurrentName(String teamId, CorrectTeamNameCommand)`, `TeamNameHistory updateNameHistory(String teamId, Long historyId, TeamNameHistoryCommand)`, `void deleteNameHistory(String teamId, Long historyId)`, `Team disbandTeam(String teamId, LocalDate disbandedAt)`
  - `PersonManagementUseCase`: `Person createPerson(PersonCommand)`, `Person updatePerson(String personId, PersonCommand)`, `void deletePerson(String personId)`, `PersonSummoner linkSummoner(String personId, String puuid, LinkSummonerCommand)`, `void unlinkSummoner(String personId, String puuid)`, `List<PersonSummoner> getSummoners(String personId)`
  - 서비스 규칙: 생성 시 `exists` → `ESPORTS_ID_DUPLICATED`; 없는 대상 → 해당 `*_NOT_FOUND`; 팀 생성·수정 시 `parentTeamId` 가 있으면 부모를 조회해 `Team.validateParent`(없으면 `ESPORTS_TEAM_NOT_FOUND`); 해체는 `hasContractEffectiveOnOrAfter(teamId, disbandedAt + 1)` 이면 `TEAM_HAS_ACTIVE_CONTRACT`; 인물 삭제는 `hasRecords` 면 `PERSON_IN_USE`; 소환사 연결은 puuid 가 다른 인물에 있으면 `SUMMONER_ALREADY_LINKED`, 같은 인물이면 수정
  - 경로(`/api/v1/admin/esports` 아래): 스펙 7.2 표 그대로 + `GET /persons/{personId}/summoners`(비공개 포함 전체 — 관리 화면용)
  - Request 레코드(Bean Validation): ID 는 `@NotBlank @Pattern(regexp = "^[a-z0-9_]{1,30}$")` (league 50, season 80 자), `nationality` 는 `@Pattern(regexp = "^[A-Z]{2}$")`, 이름류 `@NotBlank`, 날짜 `@NotNull` 는 개명일·해체일만. `LinkSummonerRequest(boolean main, Boolean visible /* null → true */, @Size(max=300) String note)`
  - Response: 도메인을 그대로 옮긴 레코드 (`LeagueResponse.from(League)` 등)

- [ ] **Step 1: 실패하는 테스트 작성**
  - 서비스(Mockito): `createTeam_ID중복이면_ESPORTS_ID_DUPLICATED`, `createTeam_상위팀에_상위팀이_있으면_TEAM_NESTED_TOO_DEEP`, `renameTeam_이력을_저장하고_팀을_갱신한다`, `disbandTeam_해체일이후_계약이있으면_TEAM_HAS_ACTIVE_CONTRACT`, `deletePerson_기록이있으면_PERSON_IN_USE`, `linkSummoner_다른인물에_연결된_puuid면_SUMMONER_ALREADY_LINKED`
  - 컨트롤러(RestDocs, 문서 id `admin-esports-<동작>`): 각 엔드포인트 성공 1건 + `POST /teams` 본문 검증 실패 400 1건. 상태 코드: 생성 201, 수정·개명·정정·해체·연결 200, 삭제 204

- [ ] **Step 2: 실패 확인** — `./gradlew :module:domain:esports:test --tests '*MasterService*' --tests '*PersonManagementService*' --tests '*Admin*ControllerTest'` → 컴파일 실패
- [ ] **Step 3: 구현**
- [ ] **Step 4: 통과 확인** — 위 명령 PASS
- [ ] **Step 5: 커밋** — `git commit -m "feat: e스포츠 리그·시즌·팀·인물 관리자 API를 추가한다"`

---

### Task 8: 조회 어댑터 — 뷰 읽기

**Files:**
- Create: `application/port/out/EsportsQueryPort.java`, `application/model/readmodel/*.java`, `application/command/PersonSearchCommand.java`
- Create: `adapter/out/persistence/query/EsportsQueryAdapter.java` (+ 필요한 RowMapper)
- Test: `adapter/out/persistence/query/EsportsQueryAdapterTest.java`

**Interfaces:**
- Consumes: Task 2 뷰 컬럼, Task 3 enums
- Produces (ReadModel 은 모두 record):
  - `LeagueReadModel(String leagueId, String name, String nameAcronym, String region, boolean international, String imageUrl, String darkImageUrl, int sortOrder, boolean active)`
  - `SeasonReadModel(String seasonId, String leagueId, String name, String nameAcronym, Integer year, LocalDate startDate, LocalDate endDate, int sortOrder)`
  - `TeamReadModel(String teamId, String teamCode, String name, String nameEng, String imageUrl, String darkImageUrl, String homeLeagueId, String parentTeamId, LocalDate foundedAt, LocalDate disbandedAt, LocalDate nameSince)`
  - `TeamNameReadModel(Long historyId, String teamCode, String name, String nameEng, String imageUrl, String darkImageUrl, LocalDate validFrom, LocalDate validTo)`
  - `TeamDetailReadModel(TeamReadModel team, List<TeamNameReadModel> pastNames, List<TeamReadModel> organizationTeams)` — organizationTeams = 같은 조직의 다른 팀(자기 제외)
  - `RosterEntryReadModel(String personId, String nickName, String imageUrl, String nationality, Role role, Position position, ContractType contractType, LocalDate startDate, LocalDate lastDay, boolean onLoan, String loanFromTeamId, boolean inactive, HiatusReason inactiveReason)`
  - `LoanedOutReadModel(String personId, String nickName, Position position, String loanTeamId, LocalDate loanLastDay)`
  - `TeamRosterReadModel(String teamId, List<RosterEntryReadModel> players, List<RosterEntryReadModel> staff, List<LoanedOutReadModel> loanedOut)` — players 정렬 TOP·JGL·MID·AD·SPT 후 시작일, staff 정렬 HEAD_COACH 먼저
  - `TenureReadModel(String personId, String nickName, String organizationId, boolean asPlayer, LocalDate startDate, LocalDate lastDay, EndReason endReason, int contractCount, int renewalCount, boolean loan, List<Role> roles, List<String> teamIds)`
  - `TeamMemberReadModel(TenureReadModel tenure, LocalDate teamStartDate, LocalDate teamLastDay)` — 이 팀 계약들의 최소 시작일·최대 마지막 날
  - `SeasonRosterReadModel(String seasonId, String teamId, String teamCodeAtSeason, String teamNameAtSeason, String personId, String nickName, Role role, Position position, ContractType contractType, LocalDate startDate, LocalDate lastDay)` — 당시 팀명 = `v_esports_team_name_period` 에서 시즌 `start_date`(NULL 이면 현재 이름)가 기간 안에 드는 행
  - `PersonStatusReadModel(PersonStatus status, String teamId, String contractTeamId, Role role, Position position, LocalDate contractLastDay, boolean inactive, HiatusReason inactiveReason, boolean retiredAsPlayer, String nextTeamId, LocalDate nextStartDate, String rightsTeamId, LocalDate rightsUntil)`
  - `PersonSummaryReadModel(String personId, String nickName, String realName, String nationality, String imageUrl, String teamCode, PersonStatusReadModel status)` — status·teamCode 는 이력 없으면 null
  - `SummonerLinkReadModel(String puuid, boolean main, boolean visible, String note)`
  - `PersonProfileReadModel(String personId, String nickName, String realName, String realNameEng, LocalDate birthDate, String nationality, String imageUrl, PersonStatusReadModel status, List<SummonerLinkReadModel> summoners)` — summoners 는 visible 만, main 먼저
  - `ContractReadModel(Long contractId, String teamId, String teamCode, Role role, Position position, ContractType contractType, LocalDate startDate, LocalDate endDate, LocalDate actualEndDate, LocalDate lastDay, ContractStatus status, EndReason endReason, String note)`
  - `CareerEventReadModel(Long eventId, CareerEventType eventType, LocalDate eventDate, String teamId, HiatusReason reason, String note, String sourceUrl)`
  - `PersonCareerReadModel(String personId, List<TenureReadModel> tenures, List<ContractReadModel> contracts, List<CareerEventReadModel> events)` — 모두 시작일·사건일 오름차순
  - `RosterIssueReadModel(String problem, String personId, Long contractId, String teamId, LocalDate atDate)`
  - `PersonSearchCommand(String q, PersonStatus status, Role role, String teamId, LocalDate contractEndingBy, int page, int size)` — page 기준은 기존 커뮤니티 목록과 같게
  - `EsportsQueryPort`: `List<LeagueReadModel> findLeagues()`, `List<SeasonReadModel> findSeasons(String leagueId)`, `List<TeamReadModel> findTeams(String leagueId, boolean includeDisbanded)`, `Optional<TeamDetailReadModel> findTeamDetail(String teamId)`, `TeamRosterReadModel findCurrentRoster(String teamId)`, `List<TeamMemberReadModel> findTeamMembers(String teamId)`, `List<SeasonRosterReadModel> findSeasonRoster(String seasonId, String teamId)`, `PageResult<PersonSummaryReadModel> searchPersons(PersonSearchCommand)`, `Optional<PersonProfileReadModel> findPersonProfile(String personId)`, `Optional<PersonCareerReadModel> findPersonCareer(String personId)`, `Optional<PersonSummaryReadModel> findPersonByPuuid(String puuid)`, `Optional<PersonStatusReadModel> findPersonStatus(String personId)`, `List<ContractReadModel> findContracts(String personId)`, `List<ContractReadModel> findContractsByIds(Collection<Long> ids)`, `List<RosterIssueReadModel> findRosterIssues()`
  - 검색 규칙: `q` 가 없으면 `v_esports_person_status` 에 있는 인물만, `q` 가 있으면 `person` 전체에서 `nick_name`·`real_name` ILIKE 부분 일치(상태 LEFT JOIN). `status`·`role` 은 뷰 값과 일치, `teamId` 는 `team_id` 또는 `contract_team_id` 일치, `contractEndingBy` 는 `contract_last_day <= 값`. `findPersonByPuuid` 는 `visible = TRUE` 연결만

- [ ] **Step 1: 실패하는 테스트 작성** — `EsportsQueryAdapterTest extends EsportsPostgresTestBase`. `@BeforeEach` 에서 Task 2 와 같은 모양의 픽스처(`CURRENT_DATE ± n`)를 `sql(...)` 로 넣는다(+ 리그 `lck`, 시즌 `lck_2026` [d(-300), d(-30)], 팀 `t1` 개명 이력 1행, `p_hiatus` 에 visible 계정 `puuid-v`·비공개 계정 `puuid-h`, `p_creator` 에 계정 `puuid-c`)
  - `findCurrentRoster_임차팀에는_임대선수가_있고_원소속팀에는_loanedOut으로_나온다` — t1 players 에 p_loan(`onLoan true`, `loanFromTeamId "gen"`), gen 의 loanedOut 에 p_loan(`loanTeamId "t1"`)
  - `findCurrentRoster_선수는_포지션순`
  - `findTeamMembers_T1A로_조회하면_T1A를_거친사람만` — t1a 조회에 p_move·p_up 은 있고 p_hiatus(t1 만) 는 없다. p_move 의 `teamLastDay = d(-101)`
  - `findSeasonRoster_당시_팀명을_쓴다`
  - `searchPersons_이력없는인물은_q검색에만_상태없이_나온다` — q 없는 검색에 p_creator 없음, `q = "creator"` 검색에 p_creator 있고 `status == null`
  - `searchPersons_상태와_계약종료일로_거른다` — `status CONTRACTED, contractEndingBy d(150)` 에 p_hiatus 있음, p_move(d(200)) 없음
  - `findPersonProfile_비공개계정은_빠진다` — p_hiatus summoners = [`puuid-v`]
  - `findPersonByPuuid_비공개계정은_empty`, `findPersonByPuuid_이력없는인물은_status_null` — `puuid-c` → 있음, status null
  - `findPersonCareer_소속기간_계약_사건을_시간순으로`
  - `findRosterIssues_검증뷰를_그대로_읽는다`

- [ ] **Step 2: 실패 확인** — `./gradlew :module:domain:esports:test --tests '*EsportsQueryAdapterTest'` → 컴파일 실패
- [ ] **Step 3: 구현** — `NamedParameterJdbcTemplate` + SQL 상수. enum 은 `Enum.valueOf`, 배열 컬럼은 `java.sql.Array`
- [ ] **Step 4: 통과 확인** — 위 명령 PASS (SKIPPED 없음)
- [ ] **Step 5: 커밋** — `git commit -m "feat: e스포츠 로스터 뷰 조회 어댑터를 추가한다"`

---

### Task 9: 공개 조회 API · 보안 규칙

**Files:**
- Create: `application/port/in/EsportsQueryUseCase.java`, `application/EsportsQueryService.java`
- Create: `adapter/in/web/EsportsLeagueController.java`, `EsportsTeamController.java`, `EsportsPersonController.java`, `adapter/in/web/response/*`
- Modify: `module/domain/member/src/main/java/com/example/lolserver/member/config/SecurityConfig.java`
- Modify: `module/domain/member/src/test/java/com/example/lolserver/member/config/SecurityConfigAuthorizationTest.java`
- Test: `application/EsportsQueryServiceTest.java`, `adapter/in/web/Esports*ControllerTest.java` (RestDocs)

**Interfaces:**
- Consumes: Task 8 `EsportsQueryPort`, ReadModel
- Produces:
  - `EsportsQueryUseCase`: 포트와 같은 이름의 조회 메서드. 단건 조회가 비면 `ESPORTS_TEAM_NOT_FOUND`/`ESPORTS_LEAGUE_NOT_FOUND`/`ESPORTS_SEASON_NOT_FOUND`/`PERSON_NOT_FOUND` 를 던진다. `getPersonByPuuid` 가 비면 `PERSON_NOT_FOUND`(404). `getCurrentRoster`·`getTeamMembers` 는 팀이 없으면 404
  - 경로: 스펙 7.1 표 그대로 (`GET /api/v1/esports/...`)
  - Response 는 ReadModel 을 그대로 옮긴 레코드(`*Response.from(*ReadModel)`), 목록은 `PageResponse.of(PageResult)`
  - `SecurityConfig`: 관리자 규칙 다음, 옛 챔피언 통계 규칙 앞에 `.requestMatchers(HttpMethod.GET, "/api/v1/esports/**").permitAll()` + 주석 한 줄

- [ ] **Step 1: 실패하는 테스트 작성**
  - `SecurityConfigAuthorizationTest`: `비로그인_공개` CsvSource 에 `"GET, /api/v1/esports/teams"`, `"GET, /api/v1/esports/teams/t1/roster"`, `"GET, /api/v1/esports/summoners/abc/person"`; `비로그인_401` 에 `"POST, /api/v1/esports/teams"`; 새 테스트 `어드민_esports_403` — USER 토큰으로 `POST /api/v1/admin/esports/teams` → 403
  - 서비스: `getTeamDetail_없으면_ESPORTS_TEAM_NOT_FOUND`, `getPersonByPuuid_연결없으면_PERSON_NOT_FOUND`
  - 컨트롤러(RestDocs, 문서 id `esports-<동작>`): 11개 엔드포인트 각 성공 1건 + `GET /summoners/{puuid}/person` 404 1건

- [ ] **Step 2: 실패 확인** — `./gradlew :module:domain:member:test --tests '*SecurityConfigAuthorizationTest' :module:domain:esports:test --tests '*EsportsQueryService*' --tests '*Esports*ControllerTest'` → FAIL
- [ ] **Step 3: 구현**
- [ ] **Step 4: 통과 확인** — 위 명령 PASS
- [ ] **Step 5: 커밋** — `git commit -m "feat: e스포츠 로스터 공개 조회 API를 추가한다"`

---

### Task 10: 계약 사건 · 정정 · 검증 관리자 API

**Files:**
- Create: `application/port/in/ContractEventUseCase.java`, `ContractCorrectionUseCase.java`, `application/ContractEventService.java`, `ContractCorrectionService.java`, `application/model/resultmodel/ContractEventResultModel.java`, `application/command/*`
- Create: `adapter/in/web/admin/AdminContractEventController.java`, `AdminCorrectionController.java`, `adapter/in/web/request/*`, `adapter/in/web/response/*`
- Test: `application/ContractEventServiceTest.java`, `application/ContractCorrectionServiceTest.java`, `adapter/in/web/admin/AdminContractEventControllerTest.java`, `AdminCorrectionControllerTest.java`, `adapter/ContractEventFlowTest.java` (Testcontainers 통합)

**Interfaces:**
- Consumes: Task 5 `PersonTimeline`, Task 6 `PersonTimelinePort`·`TeamPersistencePort`·`PersonPersistencePort`, Task 8 `EsportsQueryPort.findPersonStatus`·`findContractsByIds`·`findContracts`·`findRosterIssues`
- Produces:
  - `ContractEventResultModel(String personId, PersonStatusReadModel status, List<ContractReadModel> contracts, List<CareerEventReadModel> events)` — contracts = 이번에 바뀌거나 생긴 계약(`findContractsByIds`), status 는 저장 뒤 `findPersonStatus`(없으면 null)
  - Commands: `ContractCommand(String teamId, Role role, Position position, LocalDate date, LocalDate endDate, String note)`(signing·renewal·transfer), `MoveCommand(String teamId, Role role, Position position, LocalDate date, String note)`, `LoanCommand(String teamId, Position position, LocalDate date, LocalDate endDate, String note)`, `DatedCommand(LocalDate date, String note, String sourceUrl)`, `HiatusCommand(LocalDate date, HiatusReason reason, String note, String sourceUrl)`, `CorrectContractCommand(LocalDate startDate, LocalDate endDate, LocalDate actualEndDate, EndReason endReason, Position position, String note)`
  - `ContractEventUseCase`: `sign`, `renew`, `transfer`(ContractCommand) · `move`(MoveCommand) · `loan`(LoanCommand) · `recall(String personId, Long contractId, DatedCommand)` · `release`, `retire`, `unretire`, `comeBack`(DatedCommand) · `hiatus`(HiatusCommand) — 모두 `(String personId, …) → ContractEventResultModel`, `@Transactional`. 흐름: 인물 확인(`PERSON_NOT_FOUND`) → 팀 조회(`ESPORTS_TEAM_NOT_FOUND`) → `PersonTimelinePort.load` → 도메인 메서드 → `save` → 결과 조립
  - `ContractCorrectionUseCase`: `List<ContractReadModel> getContracts(String personId)`, `ContractReadModel correctContract(Long contractId, CorrectContractCommand)` (`Contract.correct` → `saveContract`), `void deleteContract(Long)`, `void deleteCareerEvent(Long)`, `List<RosterIssueReadModel> getRosterIssues()`
  - 경로: 스펙 7.3·7.4 표 그대로 (`/api/v1/admin/esports/...`). `PATCH /contracts/{contractId}` 본문은 정정 가능한 필드 전체를 보낸 값으로 덮어쓴다(`startDate` 만 `@NotNull`)
  - Request: `ContractEventRequest(@NotBlank teamId, @NotNull Role role, Position position, @NotNull LocalDate date, LocalDate endDate, @Size(max=300) note)`, `MoveRequest`, `LoanRequest(@NotBlank teamId, Position position, @NotNull date, @NotNull endDate, note)`, `DatedEventRequest(@NotNull date, note, @Size(max=500) sourceUrl)`, `HiatusRequest(@NotNull date, HiatusReason reason, note, sourceUrl)`, `CorrectContractRequest`

- [ ] **Step 1: 실패하는 테스트 작성**
  - 서비스(Mockito): `transfer_없는팀이면_ESPORTS_TEAM_NOT_FOUND`, `transfer_타임라인을_저장하고_상태를_돌려준다`(포트 호출 순서 `load → save → findPersonStatus`), `retire_없는인물이면_PERSON_NOT_FOUND`
  - `ContractEventFlowTest extends EsportsPostgresTestBase` (서비스 + 실제 어댑터): `스토브리그_재계약_이적_이동이_DB제약을_통과한다` — SIGN t1 [d(-365), d(-3)] 저장 → `renew(d(-2))` → `transfer(gen, d(30))` → `move(gena, d(60))` 각각 예외 없음, `findRosterIssues` 에 이 인물 없음
  - 정정: `correctContract_겹치면_409` — 컨트롤러 테스트에서 서비스가 `CoreException(CONTRACT_PERIOD_CONFLICT)` 를 던지면 409 + `E409`, 그리고 `ContractEventFlowTest` 에 `correctContract_겹치는기간이면_CONTRACT_PERIOD_CONFLICT`(실제 DB)
  - 컨트롤러(RestDocs, 문서 id `admin-esports-<사건>`): 사건 11개 각 성공 201 1건, 정정 200, 삭제 204, roster-issues 200, `POST /transfers` 에 `date` 누락 400 1건

- [ ] **Step 2: 실패 확인** — `./gradlew :module:domain:esports:test --tests '*ContractEvent*' --tests '*ContractCorrection*' --tests '*AdminCorrectionControllerTest'` → 컴파일 실패
- [ ] **Step 3: 구현**
- [ ] **Step 4: 통과 확인** — `./gradlew :module:domain:esports:test` → PASS (SKIPPED 없음)
- [ ] **Step 5: 커밋** — `git commit -m "feat: e스포츠 계약 사건·정정·검증 관리자 API를 추가한다"`

---

### Task 11: 회원 역할 노출 · 문서 · 전체 검증

**Files:**
- Modify: `module/domain/member/src/main/java/com/example/lolserver/member/adapter/in/web/response/MemberResponse.java` (`String role` 추가, 두 `from` 모두)
- Modify: `module/domain/member/src/test/java/com/example/lolserver/member/adapter/in/web/MemberControllerTest.java` (`data.role` 필드 문서 — 114·188행 근처 두 곳)
- Modify: `CLAUDE.md` (모듈 표에 `module/domain/esports` 행, "What to read first" 의 app 테이블 문구는 이미 반영됨), `module/app/application/CLAUDE.md` (Cross-Module 의존 목록에 `esports`)

**Interfaces:**
- Produces: `GET /api/v1/members/me` 응답 `data.role` (`"USER"`/`"ADMIN"`) — MP-188 이 쓴다

- [ ] **Step 1: 실패하는 테스트** — `MemberControllerTest` 의 `/me` 문서에 `fieldWithPath("data.role").type(JsonFieldType.STRING).description("회원 역할 (USER / ADMIN)")` 추가, 픽스처 ReadModel 에 role `"USER"`
- [ ] **Step 2: 실패 확인** — `./gradlew :module:domain:member:test --tests '*MemberControllerTest'` → FAIL (문서화되지 않은/없는 필드)
- [ ] **Step 3: 구현** — `MemberResponse` 에 `role`
- [ ] **Step 4: 통과 확인** — 위 명령 PASS
- [ ] **Step 5: 문서** — 루트 `CLAUDE.md` 모듈 표에 `| module/domain/esports | e스포츠 로스터 — 리그·시즌·팀·인물·계약 사건 관리자 API + 공개 조회. V5 뷰를 그대로 읽고 소환사는 puuid 만 다룬다 |`, app `CLAUDE.md` 의존 목록 갱신
- [ ] **Step 6: 전체 검증** — `./gradlew compileJava compileTestJava archTest test` → BUILD SUCCESSFUL, esports·app 의 Testcontainers 테스트가 SKIPPED 아님
- [ ] **Step 7: 커밋 후 push** — `git commit -m "feat: 회원 정보 응답에 역할을 추가하고 e스포츠 모듈 문서를 갱신한다"` → `git push`
