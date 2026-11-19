# e스포츠 로스터 API 설계

- 작성일: 2026-11-19
- 상태: 승인 대기
- 선행: PR #189 (app 스키마 V5 — 리그·시즌·팀·인물·계약·커리어 사건 테이블과 유도 뷰)

## 1. 의도

2026 시즌 경기가 모두 끝나고 스토브리그가 열렸다(글로벌 공식 계약 종료일 2026-11-16). 팀·선수·감독·코치를 관리하고, 최종적으로 **로스터 관리** — 계약·재계약·이적·임대·은퇴와 선수·팀 히스토리 — 를 하려 한다.

이번 작업은 V5 테이블 위에 API 를 연다. 쓰는 사람은 둘이다.

- **운영자(관리자)**: 팀·인물을 등록하고 계약 사건(입단·재계약·이적·임대·방출·은퇴…)을 입력한다.
- **방문자(공개)**: 팀 로스터, 인물 커리어, "이 소환사가 누구인지" 를 본다.

화면은 아직 없다. 화면에서 처리할 일은 Linear 이슈로 남기고, 사용자가 따로 배정한다(9절).

## 2. 목표 · 비목표 · 안티목표

**목표**
- 관리자 API: 리그·시즌·팀(개명·해체 포함)·인물·소환사 연결 관리, 계약 사건 API, 오입력 정정용 계약·사건 수정/삭제, 검증 결과 조회
- 공개 API: 리그·시즌·팀 조회, 팀 현재 로스터, 시즌 로스터, 팀 역대 인원, 인물 검색·프로필·커리어, puuid → 인물 조회
- 실제 LCK 생명주기와 맞는 상태값(4절)

**비목표** (이번에 하지 않는다)
- 경기 기록(스테이지·매치·세트·순위표)과 그에 따른 주전/후보 구분
- 전력분석관·단장 역할, 육성권 여부, 연봉·이적료
- GCD·Leaguepedia 등 외부 데이터 자동 수집
- 특정 과거 날짜 기준 로스터 조회(시즌 로스터로 대신한다)
- 크리에이터(유튜버·스트리머) 채널 정보
- 변경 감사 로그(누가 언제 바꿨는지)
- 소환사 정보 비정규화(트래픽이 늘면 별도로 — V5 주석 참고)

**안티목표** (동작해도 실패로 본다)
- 계약·사건 이력이 없는 인물(크리에이터 등)을 방문자 화면에 FA 로 보여주는 것
- 사건 API 가 중간에 실패해 반쯤 저장된 상태가 남는 것 — 사건 하나는 트랜잭션 하나다
- lol-repository 장애가 로스터 조회를 막는 것 — lol-server 는 puuid 만 다룬다
- 실제 생명주기에 없는 상태를 만들거나, 있는 상태를 표현하지 못하는 것

## 3. 제약

- **구조**: 헥사고날. 새 컨텍스트 모듈 `module/domain/esports` 를 만들고 `settings.gradle`·`module/app/application/build.gradle` 에 등록한다. 레이어 규칙은 duo 와 같은 `ArchitectureTest` 로 강제한다. esports 는 다른 컨텍스트(member·community·duo·championstats)에 의존하지 않는다.
- **DB**: app 스키마만. DDL 은 `module/app/application/src/main/resources/db/migration/app` 의 Flyway 체인. `ddl-auto: validate` 라 엔티티가 스키마와 맞아야 한다(`AppSchemaMigrationTest` 가 대조).
- **V5 는 아직 머지 전**이다(PR #189, 어느 환경에도 적용되지 않음). 6절의 스키마 변경은 새 버전(V6)이 아니라 **V5 를 직접 고친다**.
- **테스트 DB**: 기존 모듈의 영속화 테스트는 H2(create-drop)로 돈다. 로스터는 Postgres 전용 기능(EXCLUDE USING gist, daterange, 뷰)에 기대므로 **esports 의 영속화·뷰 테스트는 Testcontainers Postgres + Flyway 로 실제 app 체인을 적용해서** 돌린다(Docker 가 없으면 건너뛰는 기존 `AppSchemaMigrationTest` 방식).
- **보안**: 규칙은 member 모듈의 `SecurityConfig` 에 있다. `/api/v1/admin/**` 은 이미 `hasRole("ADMIN")`. 공개 조회 `GET /api/v1/esports/**` 를 `permitAll` 로 추가한다. ADMIN 은 DB 에서 `member.role` 을 바꿔 부여한다(이번 범위 밖).
- **ID**: `league_id`·`season_id`·`team_id`·`person_id` 는 운영자가 정하는 사람이 읽는 고정 키(`lck`, `lck_2026`, `t1`, `faker`)다. 생성 후 바꾸지 않는다.
- **날짜**: 모든 기간은 양 끝 포함. 계약 종료일은 GCD 공개값(UTC 기준 날짜)을 그대로 쓴다 — 2026 시즌은 `2026-11-16`. 사건 API 의 날짜는 **효력 발생일**(KST 날짜)이다.
- **공통 관례**: `ResponseEntity<ApiResponse<T>>`, POST 201 / GET·PUT·PATCH 200 / DELETE 204, `CoreException(ErrorType)`, 도메인 규칙은 도메인 객체 `validate*` 가 던진다, ReadModel 은 `*ReadModel.of(...)`, 컨트롤러는 RestDocs 테스트(`RestDocsSupport`).
- **소환사**: lol-server 는 `puuid` 만 저장·반환한다. Riot ID·티어 등은 화면이 lol-repository 공개 API(`GET /api/v1/summoners/{puuid}`, `GET /api/v1/summoners/by-riot-id/{gameName}`)로 받는다.

## 4. 실제 생명주기와 상태값

근거: LCK 공식 규정집(2024-12-16 개정, 2025-01-15 시행). 2026년판은 공개본을 찾지 못했다.

### 4.1 실제 생명주기 → 모델

| 실제 (규정집) | 모델 |
|---|---|
| 영입 경로는 트레이드·비계약 선수(FA) 영입·육성권 선수·임대 넷뿐 | 계약 시작 유형 `TRANSFER`·`SIGN`·`SIGN`(육성권은 SIGN 의 한 종류)·`LOAN` |
| 재계약·연장은 언제든 가능 | `RENEWAL` (만료 전 연장이면 직전 계약을 `RENEWED` 로 조기 종료) |
| 모든 계약은 글로벌 공식 계약 종료일(11월 세 번째 월요일 23:59 UTC)에 끝남, 최대 4시즌 | `end_date` = GCD 종료일 |
| 상호 해지(서면 합의) | 종료 사유 `RELEASED` |
| 트레이드 = 계약 중 권리 양도 | 직전 계약 `TRANSFERRED` + 새 `TRANSFER` 계약 |
| 임대: LCK 팀끼리만, 최대 1시즌, 원 소속 로스터에 남음, 조기 복귀는 예외, 끝날 때 구매 선택권 | `LOAN` 계약이 원 계약과 겹침. 조기 복귀 `RECALLED`. 구매 선택권은 이적으로 처리 |
| 계약 중 은퇴 → 남은 기간 동안 GCD 에 은퇴 선수로 남고, 복귀하면 원 소속팀이 권리를 가짐 | 계약을 `RETIRED` 로 조기 종료 + `RETIRE` 사건. 권리 기간 = `actual_end_date` 다음 날 ~ `end_date` |
| 원 소속팀 동의 없는 임의 탈퇴는 만료일까지 FA 아님 | 별도 상태 없음. 계약을 끝내지 않고 `HIATUS`(PERSONAL) 로 남긴다 |
| 계약 유지 중 비활동: 휴식, 출장정지, 군 입대 | `HIATUS` 사건 + 사유 |
| 통합 로스터(1군·2군 한 명단) + 경기별 LCK/CL 엔트리, CL 팀은 이름이 따로 있음(T1 Esports Academy = T1A) | 팀을 나눈다 — 5절 |
| 감독·코치는 LCK/CL 소속을 구분해 등록, 언제든 변경 | 스태프도 CL 팀 계약. 변경은 팀 내 이동 |
| 플레잉코치는 선수 계약을 맺고 선수로만 등록 | `PLAYER` |

### 4.2 상태값

| 이름 | 값 | 저장/유도 |
|---|---|---|
| 인물 상태 `PersonStatus` | `CONTRACTED` 계약 중 · `ON_LOAN` 임대 중 · `FREE_AGENT` 계약 없음 · `RETIRED` 선수 은퇴 | 유도 (`v_esports_person_status`) |
| 역할 `Role` | `PLAYER` · `HEAD_COACH` · `COACH` | 계약에 저장 |
| 포지션 `Position` | `TOP` · `JGL` · `MID` · `AD` · `SPT` (선수만, 필수) | 계약에 저장 |
| 계약 상태 `ContractStatus` | `UPCOMING` · `ACTIVE` · `ENDED` | 유도 |
| 계약 시작 유형 `ContractType` | `SIGN` · `TRANSFER` · `RENEWAL` · `LOAN` · **`MOVE`(신규, 팀 내 이동)** | 저장 |
| 종료 사유 `EndReason` | `EXPIRED`(유도만) · `RENEWED` · `TRANSFERRED` · `RELEASED` · `RETIRED` · `RECALLED` · **`MOVED`(신규)** | 조기 종료일 때만 저장 |
| 커리어 사건 `CareerEventType` | `RETIRE` · `UNRETIRE` · `HIATUS` · `RETURN` | 저장 |
| 비활동 사유 `HiatusReason` **(신규)** | `REST` · `SUSPENSION` · `MILITARY` · `PERSONAL` | `HIATUS` 에만, 선택 |

인물 상태 규칙(오늘 기준):
1. 진행 중인 `LOAN` 계약이 있으면 `ON_LOAN` (임대팀 기준으로 보인다)
2. 진행 중인 계약(임대 제외)이 있으면 `CONTRACTED` — 역할은 그 계약의 역할
3. 마지막 `RETIRE`/`UNRETIRE` 사건이 `RETIRE` 면 `RETIRED`
4. 그 밖은 `FREE_AGENT`
5. 계약·사건이 하나도 없는 인물은 상태가 없다(목록·상태 조회에서 빠진다)

함께 내려주는 보조 정보: `role`, `teamId`, `position`, `contractLastDay`, `inactive`(마지막 `HIATUS`/`RETURN` 이 오늘 이전 `HIATUS`), `inactiveReason`, `retiredAsPlayer`(은퇴 후 코치가 된 경우 등), `nextContract`(확정된 다음 계약의 팀·시작일), `rightsTeamId`·`rightsUntil`(계약 중 은퇴한 선수의 원 소속팀 권리 — 마지막 `RETIRED` 계약의 `end_date` 가 오늘 이후일 때만).

계약 중 은퇴 선수의 권리 기간 (내 판단):
- 권리 기간 중 다른 조직과의 `SIGN` 은 막지 않는다(권리 양도·합의로 풀리는 경우가 있다). 대신 검증 뷰가 `SIGN_DURING_RETIRE_RIGHTS` 로 보여준다.
- 원 소속팀 복귀는 `/unretirements` 후 `/signings`(원 소속팀, `end_date` 는 입력값).

현재 V5 뷰의 `PLAYER`/`STAFF` 구분은 상태가 아니라 역할이라 위처럼 나눈다.

## 5. 팀 분리 — T1 / T1 Esports Academy

**결정(사용자)**: 1군·2군은 팀을 나눠 표현한다. LCK CL 에는 실제로 다른 이름의 팀이 있다(T1 Esports Academy, Gen.G Global Academy…).

- `esports_team.parent_team_id` 를 추가한다. CL 팀 → LCK 팀(같은 조직). 1단계만 허용한다(부모의 부모 없음, 자기 자신 금지). CL 팀의 `home_league_id` 는 CL 리그(`lck_cl`).
- 계약은 선수가 실제로 뛰는 팀(T1 또는 T1A)에 건다.
- **콜업·샌드다운 = 팀 내 이동(`MOVE`)**. 실제 계약서는 하나이므로:
  - 직전 계약을 이동일 전날로 끝내고 사유 `MOVED`
  - 이동한 팀에 `MOVE` 계약을 이동일부터 시작, **`end_date` 는 직전 계약 값을 그대로**, 역할도 그대로(포지션은 바뀔 수 있다)
  - 같은 조직(부모·자식·형제) 안에서만 가능
- **재계약은 같은 조직이면 된다**. "재계약하면서 1군 승격" 은 T1A 계약 만료 다음 날 T1 에 `RENEWAL` 한 줄로 넣는다.
- **소속 기간(`v_esports_tenure`)은 조직 단위로 묶는다** (`COALESCE(parent_team_id, team_id)`). `MOVE`·`RENEWAL` 로 이어진 계약은 한 줄이다. 한 줄 안의 팀 구간(T1A → T1)은 계약 이력에서 보인다.
- 이적·임대의 "다른 팀" 판단도 조직 단위다(같은 조직끼리는 이적·임대가 아니라 이동).
- 현재 로스터는 팀 단위로 조회한다(T1 = LCK 로스터, T1A = CL 로스터). 팀 상세는 같은 조직의 다른 팀(부모·자식)을 함께 내려준다.

## 6. 스키마 변경 (PR #189 의 V5 를 고친다)

1. `esports_team.parent_team_id VARCHAR(30) NULL` + 자기 참조 FK + `CHECK (parent_team_id <> team_id)`. "부모의 부모 없음" 은 CHECK 로 못 막으므로 도메인이 검사하고 검증 뷰가 잡는다.
2. `ck_contract_type` 에 `MOVE`, `ck_contract_end_reason` 에 `MOVED` 추가.
3. `esports_career_event.reason VARCHAR(20) NULL` + `CHECK (reason IS NULL OR (event_type = 'HIATUS' AND reason IN ('REST','SUSPENSION','MILITARY','PERSONAL')))`.
4. `v_esports_person_status` 를 4.2 규칙으로 다시 쓴다 (`status` 값, `role`, `inactive`, `inactive_reason`, `retired_as_player`, `next_team_id`, `next_start_date`, `rights_team_id`, `rights_until`). `v_esports_current_roster` 는 그대로 둔다(임대 선수는 임차팀에 보인다). 원 소속팀의 `loanedOut` 은 `v_esports_contract_status` 에서 따로 고른다.
5. `v_esports_contract_status` 의 `RENEWED` 유도: "같은 팀" → "같은 조직". 또 다음 날 같은 조직 `MOVE` 가 이어지면 자연 만료가 아니므로 `MOVED` 가 이미 저장돼 있다(조기 종료) — 변경 없음.
6. `v_esports_tenure` 파티션을 `team_id` → 조직 ID 로. 출력에 `org_team_id` 와 구간에 포함된 `team_ids` 배열.
7. `v_esports_roster_invalid` 에 검사 추가: `MOVE_NOT_CONTINUOUS`(직전 계약이 전날 `MOVED` 로 끝나지 않음), `MOVE_OUTSIDE_ORG`, `MOVE_END_DATE_CHANGED`, `MOVE_PLAYER_STAFF_SWITCH`, `RENEWAL_NOT_CONTINUOUS` 를 조직 단위로, `TEAM_NESTED_TOO_DEEP`, `SIGN_DURING_RETIRE_RIGHTS`.
8. V5 머리 주석의 예시 날짜 `~2026-11-17 / 2026-11-18~` → `~2026-11-16 / 2026-11-17~`.
9. `AppSchemaMigrationTest` 에 새 제약(MOVE·reason CHECK, parent 자기 참조) 회귀 검사 추가.

## 7. API

공개: `GET /api/v1/esports/**` (인증 없음). 관리자: `/api/v1/admin/esports/**` (ADMIN).

### 7.1 공개 조회

| 메서드·경로 | 내용 |
|---|---|
| `GET /api/v1/esports/leagues` | 리그 목록 (지역 리그 / 국제 대회 구분, 정렬 순서) |
| `GET /api/v1/esports/leagues/{leagueId}/seasons` | 시즌 목록 |
| `GET /api/v1/esports/teams?leagueId=&includeDisbanded=` | 팀 목록 (기본: 해체 팀 제외) |
| `GET /api/v1/esports/teams/{teamId}` | 팀 상세: 현재 이름·코드·로고, 지나간 이름 목록, 같은 조직의 팀 |
| `GET /api/v1/esports/teams/{teamId}/roster` | 현재 로스터: `players`(포지션 순, 임대 온 선수 표시)·`staff`·`loanedOut`(이 팀 계약인데 지금 다른 팀에 임대 간 선수와 임차팀). 실제로 임대 선수는 원 소속 통합 로스터에도 남으므로 숨기지 않고 따로 보여준다 |
| `GET /api/v1/esports/teams/{teamId}/members` | 역대 인원: 이 팀을 거친 사람의 소속 기간(조직 단위로 묶인 한 줄)과 그 안의 이 팀 구간. T1A 로 조회하면 T1A 를 거친 사람만, T1 로 조회하면 T1 을 거친 사람만 나온다. 선수/스태프 구분 |
| `GET /api/v1/esports/seasons/{seasonId}/rosters?teamId=` | 시즌 로스터 (계약 기간 ∩ 시즌 기간), 그 시즌 당시 팀명 포함 |
| `GET /api/v1/esports/persons?q=&status=&role=&teamId=&contractEndingBy=&page=&size=` | 인물 검색 (활동명·실명). 상태 없는 인물은 `q` 검색에만 나온다 |
| `GET /api/v1/esports/persons/{personId}` | 프로필 + 현재 상태 + 공개 소환사 puuid 목록(`visible`만, 대표 계정 먼저) |
| `GET /api/v1/esports/persons/{personId}/career` | 소속 기간 + 계약 이력(상태·종료 사유 포함) + 커리어 사건, 시간순 |
| `GET /api/v1/esports/summoners/{puuid}/person` | 이 소환사가 누구인지: 인물 요약 + 현재 상태. 연결 없음·비공개면 404 |

### 7.2 관리자 — 마스터 데이터

| 메서드·경로 | 내용 |
|---|---|
| `POST /leagues`, `PUT /leagues/{leagueId}` | 리그 등록·수정 |
| `POST /leagues/{leagueId}/seasons`, `PUT /seasons/{seasonId}` | 시즌 등록·수정 |
| `POST /teams`, `PUT /teams/{teamId}` | 팀 등록·수정(이름·코드·로고 제외, `parent_team_id` 포함) |
| `POST /teams/{teamId}/renames` | 개명: 현재 이름·코드·로고를 이력으로 옮기고(`valid_to` = 개명일 전날) 새 값 + `name_since` 를 한 트랜잭션으로 |
| `PATCH /teams/{teamId}/current-name` | 현재 이름·코드·로고·`name_since` 오입력 정정(이력을 남기지 않는다) |
| `PUT /teams/{teamId}/name-history/{historyId}`, `DELETE …` | 지나간 이름 이력 정정·삭제 |
| `POST /teams/{teamId}/disbandment` | 해체: `disbanded_at`. 진행 중 계약이 있으면 거부 |
| `POST /persons`, `PUT /persons/{personId}`, `DELETE /persons/{personId}` | 인물 등록·수정·삭제(계약·사건·소환사 연결이 없을 때만 삭제) |
| `PUT /persons/{personId}/summoners/{puuid}`, `DELETE …` | 소환사 연결·수정(`is_main`, `visible`, `note`)·해제. 대표 계정을 바꾸면 이전 대표는 자동 해제. 이미 다른 인물에 연결된 puuid 는 409 |

(경로 앞의 `/api/v1/admin/esports` 생략)

### 7.3 관리자 — 계약 사건

모두 `POST /api/v1/admin/esports/persons/{personId}/…`. 요청의 `date` 는 효력 발생일. 응답은 처리 후 인물 상태 + 이번에 생기거나 바뀐 계약·사건(201).

**판단 기준 — 효력 발생일 (내 판단)**
- 규칙은 오늘이 아니라 `date` 기준으로 그 인물의 전체 타임라인을 보고 판단한다. 도메인 객체는 "현재 상태 기계" 가 아니라 "타임라인 검증기" 다.
  - "진행 중 계약" = `date` 전날(또는 표에 적은 날) 유효한 계약, "이미 은퇴/비활동" = `date` 이전 마지막 사건 기준.
- 과거 날짜(지난 이력 입력)와 미래 날짜(발표 후 효력 전, 예: 11-19 발표 · 11-25 효력)를 모두 받는다.
- 더 나중 기록이 있는 상태에서 그 이전 날짜 사건을 끼워 넣을 수 있다. 단 뒤 기록과 기간이 겹치면 거부한다(도메인 검사 + DB EXCLUDE 가 최종 방어).
- 지난 이력은 이 API 를 반복 호출해 채우거나 SQL 로 넣는다. 일괄 가져오기 API 는 만들지 않는다.

| 경로 | 사건 | 서버가 하는 일 | 거부 조건 (예) |
|---|---|---|---|
| `/signings` | 신규 입단 (FA 영입·신인·스태프 합류) | `SIGN` 계약 생성 | 진행 중 계약(임대 제외)과 겹침, 선수 계약인데 은퇴 상태 |
| `/renewals` | 재계약·연장 | `date` 전날 유효한 같은 조직 계약(임대 제외)을 잇는 `RENEWAL`. 그 계약의 마지막 날이 `date` 전날이면 그대로 이어 붙이고(만료 후 재계약 — 스토브리그의 기본), 더 뒤면(만료 전 연장) 그 계약을 `date` 전날 `RENEWED` 로 조기 종료. 같은 조직의 다른 팀으로 재계약해도 된다(재계약 + 승격) | `date` 전날 유효한 같은 조직 계약 없음 — 공백 뒤 같은 팀과 다시 맺으면 `/signings` |
| `/transfers` | 이적(트레이드) | 현재 계약을 전날 `TRANSFERRED` 로 종료 + 다른 조직에 `TRANSFER`. 이적 팀에 진행 중 임대가 있으면 그 임대도 전날 `TRANSFERRED`(구매 선택권) | 진행 중 계약 없음, 같은 조직 |
| `/moves` | 조직 안 변경: 팀 이동(콜업·샌드다운·스태프 LCK/CL 소속 변경), 스태프 역할 변경(코치 ↔ 감독, 감독대행), 선수 포지션 전향 | 현재 계약을 전날 `MOVED` 로 종료 + 같은 조직에 `MOVE`(`end_date` 유지). 팀·역할·포지션 중 하나 이상이 바뀌어야 한다 | 다른 조직, 바뀐 것 없음, 선수 ↔ 스태프 변경(선수 계약과 코칭스태프 계약은 다른 계약 — 방출·은퇴 후 `/signings`), 임대 중 |
| `/loans` | 임대 | 임차팀에 `LOAN` 계약(원 계약 유지) | 선수 아님, 원 계약 없음, 같은 조직, 임대 종료일 > 원 계약 마지막 날, 이미 임대 중 |
| `/loans/{contractId}/recall` | 임대 조기 복귀 | 임대 계약을 그날 `RECALLED` 로 종료 | 임대 계약 아님, 이미 끝남 |
| `/releases` | 방출·상호 해지 | 진행 중 계약(임대 중이면 임대 포함)을 그날 `RELEASED` 로 종료 | 진행 중 계약 없음 |
| `/retirements` | 선수 은퇴 | 진행 중 선수 계약이 있으면 그날 `RETIRED` 로 종료 + `RETIRE` 사건 | 이미 은퇴, 진행 중 계약이 스태프 |
| `/unretirements` | 은퇴 번복 | `UNRETIRE` 사건 | 은퇴 상태 아님 |
| `/hiatuses` | 휴식·출장정지·군 입대 | `HIATUS` 사건 + 사유 | 이미 비활동 |
| `/returns` | 활동 재개 | `RETURN` 사건 | 비활동 아님 |

이적·이동·방출·은퇴처럼 진행 중 계약이 둘(원 계약 + 임대)일 수 있는 사건은 요청에 대상 `contractId` 를 받지 않고 규칙으로 정한다(위 표). 규칙은 인물 한 명의 계약·사건 묶음을 가진 도메인 객체가 검사한다.

### 7.4 관리자 — 정정 · 검증

| 메서드·경로 | 내용 |
|---|---|
| `GET /persons/{personId}/contracts` | 계약 원본 목록(정정 화면용) |
| `PATCH /contracts/{contractId}` | 오입력 정정: 날짜·포지션·종료일·조기 종료·메모. 규칙 검사는 도메인, 겹침은 DB 제약이 최종 방어 |
| `DELETE /contracts/{contractId}` | 잘못 넣은 계약 삭제 |
| `DELETE /career-events/{eventId}` | 잘못 넣은 사건 삭제 |
| `GET /roster-issues` | `v_esports_roster_invalid` 결과(문제 유형·인물·계약·팀·날짜) |

정정·삭제는 사건 규칙을 거치지 않으므로 앞뒤가 어긋날 수 있다. 어긋나면 `roster-issues` 가 보여준다.

### 7.5 오류

공통 `CoreExceptionAdvice` 는 `ErrorType` 의 고정 문구만 응답한다(`CoreException(type, message)` 의 message 는 버려진다). 공유 커널은 바꾸지 않고, **거부 사유마다 `ErrorType` 을 따로 둔다** (내 판단).

- 404: `ESPORTS_LEAGUE_NOT_FOUND`, `ESPORTS_SEASON_NOT_FOUND`, `ESPORTS_TEAM_NOT_FOUND`, `PERSON_NOT_FOUND`, `CONTRACT_NOT_FOUND`, `CAREER_EVENT_NOT_FOUND`, `TEAM_NAME_HISTORY_NOT_FOUND`
- 409: `ESPORTS_ID_DUPLICATED`, `SUMMONER_ALREADY_LINKED`, `CONTRACT_PERIOD_CONFLICT`(기간 겹침), `PERSON_IN_USE`(삭제하려는 인물에 기록 있음), `TEAM_HAS_ACTIVE_CONTRACT`(해체 거부)
- 400 — 사건 거부 사유별: `NO_ACTIVE_CONTRACT`, `NOT_A_CONTINUATION`(재계약할 직전 계약 없음), `SAME_ORGANIZATION`(이적·임대를 같은 조직에), `DIFFERENT_ORGANIZATION`(이동을 다른 조직에), `NOTHING_CHANGED`, `PLAYER_STAFF_SWITCH`, `NOT_A_PLAYER`, `ALREADY_ON_LOAN`, `LOAN_EXCEEDS_CONTRACT`, `NOT_A_LOAN`, `PERSON_RETIRED`, `PERSON_NOT_RETIRED`, `ALREADY_INACTIVE`, `NOT_INACTIVE`, `TEAM_NESTED_TOO_DEEP`
- 없던 `DataIntegrityViolationException` 처리기는 공통에 넣지 않는다. esports 영속화 어댑터가 제약 이름을 보고 `CONTRACT_PERIOD_CONFLICT`(EXCLUDE)·`ESPORTS_ID_DUPLICATED`(PK·UNIQUE)·`INVALID_INPUT`(CHECK)으로 바꿔 던진다. 다른 모듈의 응답은 그대로다.

## 8. 접근 방식 (검토한 대안)

| 결정 | 고른 것 | 버린 것과 이유 |
|---|---|---|
| 계약 입력 | **사건마다 API** (사용자 선택) | 계약 생성·수정·삭제만: 이적처럼 두 행을 바꾸는 일을 화면이 두 번 호출해야 하고, 실패하면 반쯤 저장된 상태가 남는다 |
| 1군·2군 | **팀 분리 + 팀 내 이동** (사용자 선택) | 배치 이력 테이블: 계약과 별도 표를 하나 더 관리해야 한다. 구분 안 함: T1 로스터에 CL 선수가 섞인다 |
| 조회 계산 | **V5 뷰를 그대로 읽는다** (내 판단) | 자바에서 다시 계산: 만료/재계약 유도·소속 기간 묶기·임대 표시를 두 곳에 두게 되어 어긋난다 |
| 소환사 정보 | **화면이 lol-repository 공개 API 로** (내 판단, 사용자의 "puuid 만 받고 조회 시 요청" 결정과 같다) | lol-server 가 대신 호출: 새 의존과 장애 전파가 생긴다 |
| 스키마 변경 위치 | **PR #189 의 V5 수정** (내 판단) | V6 추가: 머지 전 버전을 두고 곧바로 고치는 마이그레이션이 생긴다 |

## 9. 화면에서 할 일 (Linear 이슈로 남김)

설계 승인 후 Linear 에 이슈로 만든다. 사용자가 따로 배정한다.

1. 관리자 진입·권한(ADMIN 만 메뉴 노출, 403 처리)
2. 리그·시즌·팀 관리 화면(개명·해체·같은 조직 팀 지정 포함)
3. 인물 관리 + 소환사 연결: Riot ID 로 lol-repository `by-riot-id` 를 불러 puuid 를 얻어 연결 API 에 넘김
4. 계약 사건 입력 화면(입단·재계약·이적·팀 내 이동·임대·임대 복귀·방출·은퇴·은퇴 번복·휴식·복귀), 정정 화면, 검증 결과 화면
5. 공개: 팀 로스터 페이지(LCK 팀 / CL 팀 전환), 팀 역대 인원, 시즌 로스터
6. 공개: 인물 프로필·커리어 타임라인, 소환사 정보는 lol-repository `GET /api/v1/summoners/{puuid}`
7. 전적 검색 화면: puuid → 인물 배지("T1 · Faker")
8. 스토브리그 화면: FA 목록, 계약 만료 예정자

## 10. 테스트

- 도메인: 사건별 규칙(정상·거부) 단위 테스트
- 영속화·뷰: Testcontainers Postgres 에 app 체인 적용 후 어댑터·뷰 결과 검증(3절 제약)
- 스키마: `AppSchemaMigrationTest` 회귀(6절 9)
- 웹: 공개·관리자 컨트롤러 RestDocs 테스트, `SecurityConfig` 경로 규칙 테스트(공개 GET 은 인증 없이 200, 관리자 경로는 USER 403)
- `ArchitectureTest`(esports)

## 11. 진행 순서

1. PR #189 브랜치(`feat/esports-roster-schema`)에 6절 스키마 변경 커밋
2. 새 브랜치 `feat/esports-roster-api`(#189 위)에서 API 구현, PR 대상은 #189 브랜치 → #189 머지 후 `develop` 으로 변경
3. Linear UI 이슈 생성(9절)

## 12. 빌더에게 맡기는 것

- 응답 DTO 필드 이름·구성, 페이지 응답 형식(기존 `PageResponse`/`SliceResponse` 중 선택)
- 뷰를 읽는 방법(`@Immutable` 엔티티 / QueryDSL / 네이티브 쿼리)
- 클래스·UseCase 나누기, 캐시 헤더 여부
- 오류 메시지 문구
- 오늘 날짜 주입 방식(테스트용 `Clock` 등). 단 "오늘" 판단은 뷰의 `CURRENT_DATE` 와 같은 기준(DB 세션 시간대)이어야 한다
