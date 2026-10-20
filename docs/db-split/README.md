# lol-server app 스키마·DB 계정 런북 (MP-136 · MP-127 · MP-162)

lol-server 는 **app 스키마(회원·커뮤니티·듀오)만 소유**한다. app 밖 데이터에는 DB 권한이 없고 lol-repository API 로 받는다. 이 런북도 app 만 다룬다 — 분류는 app 테이블 목록 하나이고, 그 밖의 객체는 이름을 몰라도 되게 짰다. 근거는 [서비스 분리 설계](https://linear.app/metapick/document/14e7fbfeb5bc) 6.1·8절, [ADR 서비스 역할과 책임 경계](https://linear.app/metapick/document/d7ffc5b22f69) 3.1절, Linear 프로젝트 「Riot 데이터 도메인 lol-repository 이전」.

| 파일 | 용도 | 실행 시점 |
|---|---|---|
| [`01-roles.sql`](01-roles.sql) | `lol_server` 계정 (search_path `app`, app 밖 권한 없음, 멱등) | A-1 |
| [`02-move-schema.sql`](02-move-schema.sql) | app 테이블만 `public` → `app` 이동 + 소유자 `lol_server`, 단일 트랜잭션, app 테이블 잠금 선점 + 총 대기 5초 상한 | A-3 |
| [`02-rollback.sql`](02-rollback.sql) | 역방향 이동 + 소유자 복구 | A 롤백 |
| [`03-verify.sql`](03-verify.sql) | `lol_server` 가 app 만 소유하고 app 밖 어떤 객체에도 권한이 없는지 검증, 위반 시 실패 (읽기 전용) | 매 단계 직후 |
| [`rehearse.sh`](rehearse.sh) | 로컬 Docker 로 전 과정 리허설 | 변경 시마다 |

모든 SQL 은 슈퍼유저(현재 `POSTGRES_USER`)로 대상 DB 에서 실행한다. 어느 단계도 lol-repository 배포·설정 변경을 요구하지 않는다.

## 1. 원칙

- **app 테이블** = `member`, `member_withdrawal`, `social_account`, `community_*`, `duo_*` — 운영(V35) 13개, app 체인 V2 의 `community_image` 를 더해 14개. `02`·`02-rollback`·`03` 이 같은 술어를 쓴다. 새 app 테이블이 이 이름 규칙을 벗어나면 술어도 같이 고친다.
- **소유**: `app` 스키마와 그 안의 모든 객체(테이블·시퀀스·`app.flyway_schema_history`)는 `lol_server` 소유.
- **권한**: `lol_server` 는 app 밖에 아무 권한도 없다 (`CONNECT` + app 소유뿐).
- **DDL**: lol-server 체인 `module/app/application/src/main/resources/db/migration/app` 만. V1 = 운영(V35) app 테이블 13개 + 기준 데이터 그대로(pg_dump 비교로 확인), V2 = `community_image`(`IF NOT EXISTS`). 운영은 이동 직후 V1 을 baseline 으로 기록하고 V2 부터 적용한다. 이후 app 테이블 변경은 이 체인에 V3 부터 추가하고, lol-db-schema 에는 넣지 않는다.

## 2. 함정

1. **지금 계정으로 새 이미지를 띄우면 app 객체 소유자가 어긋난다** — Flyway 가 만드는 `app.flyway_schema_history`·V2 이후 테이블이 접속 계정(슈퍼유저) 소유가 되고, 나중에 `lol_server` 로 전환하면 거기에 쓰지 못한다.
   → prod `spring.flyway.init-sqls: SET ROLE lol_server`. 접속 계정은 슈퍼유저이거나 `lol_server` 의 멤버여야 한다 (`GRANT lol_server TO <계정>`). 덤으로 app 체인이 슈퍼유저 권한에 기대는 마이그레이션을 만들면 바로 실패한다.
2. **그 `SET ROLE` 이 앱 커넥션 풀에 남는다** — Spring Boot Flyway 는 기본적으로 앱 DataSource(Hikari)를 빌려 쓰므로, `SET ROLE` 이 걸린 커넥션이 풀로 돌아가 일부 앱 요청이 `lol_server` 권한으로 돈다. riot 모듈이 있던 이미지에서는 Hibernate 가 그 커넥션으로 app 밖 테이블을 조회하다 기동이 깨졌다 (`permission denied for table <app 밖 테이블>`, 리허설에서 재현).
   → prod `spring.flyway.user`/`password` 지정 — Spring Boot 가 Flyway 전용 커넥션(풀 없음)을 따로 만든다. `AppSchemaMigrationTest` 가 "init-sqls 가 있으면 user 도 있어야 한다"를 강제한다.
3. **app 밖 테이블에 엔티티를 만들면 기동 실패** — Hibernate `ddl-auto: validate` 는 현재 스키마(`currentSchema=app`)만 본다. app 밖을 직접 읽던 riot 모듈(gamedata·leaderboard·summoner·match)이 빠지면서 과도기 설정(`individually` + `currentSchema=app,public`)도 지웠다.
   → app 밖 데이터는 엔티티가 아니라 lol-repository API 로 받는다.
4. **이동 후 공유 체인(lol-db-schema)이 V36 이후의 app 마이그레이션을 적용하면 실패** — V36(`community_image`)은 `member`·`community_post` 를 FK 로 참조하는데 그 테이블이 `public` 에 없다 (`relation "member" does not exist`, 리허설에서 재현). app 마이그레이션은 이 레포 체인으로 옮겨졌으니 공유 체인을 쓰는 쪽(lol-repository)은 V35 에 머물거나 자기 체인으로 옮겨야 한다.

## 3. 단계

| 단계 | 내용 | 언제 | lol-server 접속 계정 |
|---|---|---|---|
| **A. app 스키마 이동** (MP-136) | `01` → `02` → 새 이미지 롤아웃 | 지금. develop 은 MP-144 이후 A 없이는 prod 에 배포할 수 없다 (가드) | 지금 계정 그대로 — Flyway 만 `SET ROLE lol_server` |
| **B. `lol_server` 계정 전환** (MP-127 · MP-162) | 자격증명 교체 | riot 모듈을 지운 이미지가 운영에 롤아웃된 뒤 | `lol_server` |

나누는 이유: `lol_server` 는 app 밖 권한이 없으므로, app 밖 테이블을 직접 읽는 모듈(gamedata·leaderboard·summoner·match, duo 의 summoner·match 호출)이 들어 있는 이미지로 전환하면 그 API 가 `permission denied` 로 깨진다. 지금 계정은 그 이미지가 운영에서 빠질 때까지 과도기로만 유지한다.

## 4. A — app 스키마 이동 (MP-136)

### 4.1 사전 조건

- [ ] lol-server 새 이미지(app 체인 + 가드 + `SET ROLE` + Flyway 전용 커넥션) 빌드 완료, **아직 롤아웃하지 않음** — 이 이미지는 이동 전 DB 에서는 의도적으로 기동에 실패한다 (`create-schemas: false` + `db/migration-guard`)
- [ ] 지금 접속 계정이 슈퍼유저이거나 `lol_server` 멤버 (2절 1)
- [ ] lol-db-schema 에 app 마이그레이션이 더 추가되지 않았음
- [ ] 직전 pg_dump 백업 (데이터는 바뀌지 않지만 카탈로그 변경이므로)

### 4.2 실행

1. `lol_server` 비밀번호를 생성해 SSM 에 둔다 (예: `/lol/prod/db-password-lol-server` — B 에서 쓴다). 슈퍼유저로 `01-roles.sql` 실행 → `03-verify.sql` 통과(P1). psql 15 이상(`\getenv`).
   - 비밀번호는 환경변수 `LOL_SERVER_DB_PASSWORD` 로 넘긴다. 명령행(`-v`, `kubectl exec -- env X=…`)에 쓰면 실행 중 `ps` 에 보이므로 파드 셸 안에서 `read -rs LOL_SERVER_DB_PASSWORD && export LOL_SERVER_DB_PASSWORD` 로 입력한다. 없으면 아무것도 바꾸지 않고 0 이 아닌 종료 코드로 끝난다.
   - 스크립트는 비밀번호가 담긴 `ALTER ROLE` 이 서버 로그·`pg_stat_statements` 에 남지 않도록 트랜잭션 안에서 기록을 끈다.
2. app 테이블을 오래 잡는 트랜잭션이 없는 때를 고른다. app 밖 작업(배치 등)은 기다릴 필요 없다 — 이동은 app 테이블만 잠근다.

   ```sql
   SELECT pid, usename, state, now() - xact_start AS xact_age, left(query, 80)
   FROM pg_stat_activity
   WHERE datname = current_database() AND state <> 'idle' AND pid <> pg_backend_pid()
   ORDER BY xact_age DESC NULLS LAST;
   ```
3. `psql -v ON_ERROR_STOP=1 -f 02-move-schema.sql`
   - app 테이블 전부를 `LOCK TABLE` 한 문장으로 먼저 잡는다. 잠금 하나당 대기 2초(`lock_timeout`), 전체 대기 5초(`statement_timeout`)가 상한이다. 대기 중엔 그 테이블 조회도 줄을 서므로 **회원·커뮤니티·듀오 요청이 멈출 수 있는 최대 시간 ≈ 5초**.
   - 잠금을 다 잡은 뒤의 이동은 수십 ms. 상한을 넘으면 `lock timeout` / `statement timeout` 으로 아무것도 바뀌지 않은 채 롤백된다 → 2단계부터 다시.
4. 출력이 app 13 테이블·11 시퀀스(`lol_server`)인지, `03-verify.sql` 이 P2 로 판별해 통과하는지 확인.
5. **lol-server 새 이미지 롤아웃**. 기동 로그에 `Successfully baselined schema with version: 1` → `Successfully applied 1 migration to schema "app", now at version v2` → `Started LolServerApplication`. 옛 파드는 커넥션 풀로 이동 후에도 동작하지만 재시작하면 `grouped` 검증으로 죽으므로 롤아웃을 미루지 않는다. 이후 `03-verify.sql` 을 다시 돌려 통과 확인 (app 14 테이블, 이력·`community_image` 까지 `lol_server` 소유).
6. 확인: 회원·커뮤니티·듀오·커뮤니티 이미지 업로드, 그리고 아직 lol-server 가 서빙하는 app 밖 API(전적 등)도 정상, 로그의 `does not exist`·`permission denied` 0건.
7. 실행 기록: 시각, 소요 시간(`\timing` 출력), 재시도 횟수를 MP-136 에 남긴다.

### 4.3 롤백

1. 새 이미지를 롤아웃했다면 **먼저 이전 이미지로 되돌린다** — 새 이미지는 app 테이블이 public 에 있으면 기동을 거부한다.
2. `psql -v ON_ERROR_STOP=1 -f 02-rollback.sql` → `03-verify.sql` (P1 으로 판별돼야 함). 잠금은 이동과 같은 방식이며 시간 초과로 실패하면 변경 없이 롤백되니 다시 실행한다. app 테이블 소유자는 실행 계정(이동 전 소유자)으로 돌아가고(다르면 `-v owner=<계정>`), V2 의 `community_image` 도 public 으로 돌아가며, `app.flyway_schema_history` 는 삭제된다 (다시 이동하면 baseline 부터 새로 쓰고 V2 는 `IF NOT EXISTS` 라 무해).

## 5. B — `lol_server` 계정 전환 (MP-127 · MP-162)

**사전 조건**: 운영 lol-server 이미지에 app 밖 테이블을 읽는 코드가 없다 — riot 모듈(gamedata·leaderboard·summoner·match)을 지우고 duo 가 Riot 계정 정보를 lol-repository 내부 API 로 받는 이미지(MP-149, MP-157·159·160)가 롤아웃돼 있다. 그 이미지는 `currentSchema=app` 만 쓴다.

1. lol-deploy: `app/templates/external-secrets.yaml` 에 lol-server 키 추가, `lol-server.yaml` 의 `DB_USERNAME`/`DB_PASSWORD` 를 `lol_server` 로 교체. `lol-repository.yaml` 은 건드리지 않는다.
2. 롤아웃 → Flyway `Schema "app" is up to date` → `Started LolServerApplication`.
3. 확인: `SELECT usename, application_name, count(*) FROM pg_stat_activity GROUP BY 1, 2;` 로 `lol_server` 접속, 로그 `permission denied` 0건, `03-verify.sql` 통과.
4. `init-sqls`·Flyway 전용 커넥션은 남겨도 무해하다 (자기 자신으로 `SET ROLE`).
5. 롤백: lol-deploy 자격증명 키를 되돌리기만 하면 된다.

## 6. 로컬 DB

local 프로필은 슈퍼유저로 접속하고 기동하며 app 스키마를 직접 만든다 (`create-schemas` 기본값 true, 가드·`SET ROLE` 없음 — 로컬엔 `lol_server` 계정이 없다).

- 빈 DB: lol-server 가 app 에 V1 부터 적용한다. lol-repository 없이 기동한다 (듀오 글·신청 생성만 lol-repository 내부 API 가 필요하다).
- 예전 DB(lol-repository 가 public 에 전부 만든 것): lol-server 가 app 에 새 테이블을 만든다. public 의 회원·게시글 등은 쓰이지 않는 사본으로 남는다 (search_path 상 app 이 먼저). 로컬 데이터를 이어 쓰려면 `INSERT INTO app.<t> SELECT * FROM public.<t>` 후 시퀀스를 맞춘다.
- lol-repository 가 공유 체인으로 public 에 app 테이블까지 만드는 동안은 로컬에서 항상 걸리므로 "public 에 app 테이블이 있으면 거부" 가드는 prod 에만 둔다.

## 부록 — 로컬 리허설

```bash
docs/db-split/rehearse.sh          # Postgres 16 컨테이너(localhost:55432)에서 이동 전 상태 → 01 → (새 이미지 거부 확인) → 잠금 경합 → 02 → 롤아웃(SET ROLE) → 계정 전환 → 롤백 → 02 → 롤아웃, 매 단계 03 검증
KEEP=1 docs/db-split/rehearse.sh   # 끝난 뒤 DB 를 남김
```

이동 전 상태는 app 체인 V1 을 `public` 에 적용해 만든다 (운영 V35 의 app 테이블과 같다 — 1절). app 밖 객체는 이름 없는 테이블 하나(`other_service`)로 대신해, 이동이 그 테이블을 잠그지 않는지와 `lol_server` 가 그 테이블을 읽지 못하는지를 본다. 아래 2026-10-20 jar 기동 확인은 lol-repository 가 만든 운영과 같은 DB 에서 했다.

2026-10-20 리허설 (Postgres 16):

| 확인 | 결과 |
|---|---|
| 다른 세션이 `member` 를 15초 잡음 | 2~3초에 `lock timeout`, 변경 없음 (P1 유지) |
| app 밖 테이블을 배타 잠금으로 쥔 채 이동 | 성공 — app 13 테이블·11 시퀀스 `lol_server` |
| 새 이미지 Flyway (슈퍼유저 + `SET ROLE lol_server`) | baseline 1 → V2, 이력·`community_image` 까지 `lol_server` 소유, `03` 통과 |
| `lol_server` 계정 전환 후 | app 체인 up to date, app 조회 가능, app 밖 테이블 `permission denied` |
| 롤백 → 재이동 → 롤아웃 | 매 단계 `03-verify` 통과 |
| `01-roles.sql` 비밀번호 환경변수 없음 | 0 이 아닌 종료 코드, 변경 없음 |
| lol-server jar, 운영과 같은 DB(app 밖 테이블이 public 에 있음) → 01 → 02, 슈퍼유저 접속 + prod Flyway 설정 | Flyway 를 앱 풀로 돌리면 app 밖 테이블에서 `permission denied` 로 실패(2절 2) / 전용 커넥션이면 baseline → V2 → `Started LolServerApplication`, app 객체 27개 `lol_server` 소유 |
| 이동 후 공유 체인 V36 적용 (2절 4) | `relation "member" does not exist` 로 실패·롤백 |
| riot 모듈 삭제 후 jar, 같은 DB → 01 → 02, 슈퍼유저 + prod Flyway 설정 | baseline → V2 → V3 → `Started LolServerApplication`, app 객체 27개 `lol_server` 소유 |
| 이어서 `lol_server` 계정으로 jar 기동 (B) | `Schema "app" is up to date` → `Started LolServerApplication`, 듀오 목록 200, `03` 통과 |
