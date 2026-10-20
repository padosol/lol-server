# lol-server DB 계정·app 스키마 분리 런북 (MP-120 · MP-127 · MP-136)

Linear 프로젝트 「DB 계정·스키마 분리」 중 **lol-server 몫**의 실행 자료. lol-server 는 자기 계정(`lol_server`)과 app 테이블만 다루고, lol-repository 의 계정·Riot 테이블·스키마는 건드리지 않는다 — lol-repository 는 따로 바뀐다 (5절 경계 조건). 근거는 [서비스 분리 설계](https://linear.app/metapick/document/14e7fbfeb5bc) 6.1·8절, [ADR 서비스 역할과 책임 경계](https://linear.app/metapick/document/d7ffc5b22f69) 3.1절.

| 파일 | 용도 | 실행 시점 |
|---|---|---|
| [`01-roles.sql`](01-roles.sql) | `lol_server` 계정, search_path, public 테이블 단위 권한 (멱등) | P1 (MP-120 작성 / MP-127 적용) |
| [`02-move-schema.sql`](02-move-schema.sql) | app 테이블만 `public` → `app` 이동, 단일 트랜잭션, app 테이블 잠금 선점 + 총 대기 5초 상한 | P2 (MP-136) |
| [`02-rollback.sql`](02-rollback.sql) | 역방향 이동 + 소유자·권한 복구 | P2 롤백 |
| [`03-verify.sql`](03-verify.sql) | 현재 단계(P1/P2)를 판별해 `lol_server` 권한·app 테이블 위치 검증, 위반 시 실패 (읽기 전용) | 매 단계 직후 |
| [`rehearse.sh`](rehearse.sh) | 로컬 Docker 로 전 과정 리허설 | 변경 시마다 |

모든 SQL 은 슈퍼유저(현재 `POSTGRES_USER`)로 대상 DB 에서 실행한다. 이 런북의 어느 단계도 lol-repository 배포·설정 변경을 요구하지 않는다.

## 1. 분류와 소유권

ADR 3.1 표 기준. 분류 규칙은 네 SQL 파일에 같은 술어로 들어 있다.

| 분류 | 테이블 | 위치 / 소유자 | `lol_server` |
|---|---|---|---|
| app (14) | `member`, `member_withdrawal`, `social_account`, `community_*`(9), `duo_*`(2) | P1: `public` / Riot 소유 계정 → P2: `app` / `lol_server` | P1: RW → P2: 소유 (RW·DDL) |
| Riot (50) | 매치·타임라인 이벤트·소환사·리그·랭킹(+`_backup`)·메타데이터·`item_meta`·e스포츠(15)·`batch_*`(6) | `public` 그대로 — 위치·소유는 lol-repository 가 정한다 | SELECT 만 (직접 읽는 동안) |
| Riot 독립 시퀀스·뷰 | `batch_*_seq`(3), `v_skill_events_flat`·`v_item_events_flat`·`v_esports_*`(6) | `public` 그대로 | 뷰 SELECT |
| 공유 체인 이력 | `public.flyway_schema_history` (V1~V35) | `public` 그대로 (lol-repository) | 없음 |
| app 체인 이력 | `app.flyway_schema_history` | `app` / `lol_server` | 소유 |
| 확장 | `btree_gist` | `public` | — |

**Riot 소유 계정** = `public.flyway_schema_history` 소유자 = lol-repository Flyway 실행 계정 (지금은 `POSTGRES_USER`). `01` 은 이 계정이 앞으로 만드는 테이블에 `lol_server` SELECT 기본 권한을 걸고, `02-rollback` 은 app 테이블 소유자를 이 계정으로 돌려준다. P1 은 소유자를 바꾸지 않고 권한만 더한다.

V35 기준 app 13 테이블·11 시퀀스(app 체인 V2 의 `community_image` 를 더하면 14·12), Riot 50 테이블·30 시퀀스·6 뷰. 새 테이블이 `member`·`social_account`·`community_*`·`duo_*` 이름 규칙을 벗어나면 Riot 으로 분류되므로 분류 규칙을 같이 고칠 것.

**app 스키마 마이그레이션은 P2 부터 lol-server 가 소유 (MP-144)**: lol-server 는 자기 체인 `module/app/application/src/main/resources/db/migration/app` 을 V1 부터 따로 갖는다. V1 = lol-db-schema V1~V35 를 적용한 app 테이블 13개(+ V32 기준 데이터) 그대로, V2 = lol-db-schema V36(`community_image`, `IF NOT EXISTS`). 이력은 `app.flyway_schema_history`. 운영은 이동 직후 lol-server 가 기동하며 V1 을 baseline 으로 기록하고 V2 부터 적용한다 (4.2). 이후 앱 테이블 변경은 lol-db-schema 가 아니라 이 체인에 V3 부터 추가한다. 세 경로(새 체인 빈 DB / 공유 V35→이동→app 체인 / 공유 V36→이동)의 app 스키마 구조·기준 데이터가 같음을 pg_dump 비교로 확인했다.

## 2. 리허설에서 드러난 함정

1. **lol-server Hibernate `ddl-auto: validate` 가 Riot 테이블을 못 찾음 → 기동 실패**
   기본 메타데이터 추출 전략(`grouped`)은 현재 스키마(`app`) 하나만 조회 → `Schema-validation: missing table [building_events]`.
   → **lol-server `postgresql-{local,prod}.yml` 에 `hibernate.hbm2ddl.jdbc_metadata_extraction_strategy: individually`** (반영됨). 테이블마다 search_path 전체에서 찾는다.
   주의: 현재 스키마에서 못 찾은 테이블은 모든 스키마에서 찾으므로, **같은 이름의 테이블이 두 스키마 이상에 있으면 기동이 실패한다** (`More than one table found`). 백업용으로 테이블을 다른 스키마에 복사해 두지 말 것. 엔티티에 `@Table(schema = ...)` 를 박는 방식은 이동 전(P1)에 스키마가 없어 기동이 깨지므로 쓰지 않는다.
2. **롤백 시 lol_server 의 앱 테이블 쓰기 권한 소실** — 소유자를 `lol_server` 에서 되돌리면 `lol_server` 앞 GRANT 가 사라진다 (`permission denied for table duo_post`). `02-rollback.sql` 이 권한을 다시 준다.
3. (로컬 전용) lol-server local 프로필의 `db/seed/season-local.sql` 은 Riot 테이블(`season`·`patch_version`)에 쓴다. 로컬에서 `lol_server` 계정으로 띄우면 실패하므로 로컬은 계속 슈퍼유저를 쓰거나 `spring.sql.init.mode=never`.
4. (lol-repository 참고) **이동 후 lol-repository 가 lol-db-schema 를 V36 이상으로 올리면 기동 실패** — V36(`community_image`)이 `member`·`community_post` 를 FK 로 참조하는데 그 테이블은 이제 `app` 에 있어 `public` 에서 못 찾는다. lol-db-schema main(`2ad34f2`)에는 V36 이 들어 있다. lol-repository 는 서브모듈을 V35(`d25aec8`)에 두거나 자기 체인으로 옮긴다 (5절).
5. (lol-repository 참고) **Riot 테이블을 `riot` 스키마로 옮길 때 Flyway 가 `riot` 에 history 를 새로 만들고 V2 부터 재적용 → 기동 실패** — 계정 search_path 첫 항목이 `riot` 이 되면 `default-schema` 미지정 Flyway 는 거기서 history 를 찾는다. 대비책 `spring.flyway.default-schema: public` 은 lol-repository PR #113 (MP-163). app 만 옮기는 이 런북에서는 생기지 않는다.

Spring Batch(`BATCH_*` 테이블·`BATCH_JOB_SEQ`), native query(lol-server match·timeline JSONB, lol-repository 랭킹 `_backup`·백필 집계 SQL), JPA·QueryDSL 은 search_path 로 코드 수정 없이 동작했다. 기존 커넥션 풀도 이동 직후 그대로 동작한다 (앱을 띄운 채 이동 리허설).

## 3. P1 — lol-server 계정 (MP-120 → MP-127)

### 3.1 lol-server 의 Riot 테이블 쓰기 0건 확인 (MP-120)

- **정적 분석**: lol-server 운영 코드에 Riot 테이블 쓰기 경로 없음. 유일한 후보 `SummonerPersistenceAdapter.save` 는 호출자 없음(죽은 코드). `@Modifying`·native DML 없음. 비-readOnly `@Transactional` 은 `SummonerService` 1곳이며 Redis·MQ 만 건드린다. 로컬 시드 SQL 만 쓴다 (2절 3).
- **`pg_stat_statements` 의 한계**: prod Postgres(StatefulSet)는 `shared_preload_libraries` 미설정이라 로드돼 있지 않고, 켜려면 DB 재시작이 필요하다. 또 지금은 두 서비스가 같은 계정이라 `userid` 로 서비스를 구분할 수 없다 → 분리 전 측정은 의미가 없다.
- **대신**: P1 적용 후 `lol_server` 는 Riot 테이블에 SELECT 만 있으므로 쓰기 시도는 즉시 `permission denied` 로 드러난다. 적용 후 1주간 lol-server 로그에서 `permission denied` 0건을 확인한다. `pg_stat_statements` 를 켜 두었다면:

```sql
SELECT calls, left(query, 120)
FROM pg_stat_statements
WHERE userid = 'lol_server'::regrole
  AND query ~* '^\s*(insert|update|delete|merge|truncate)'
ORDER BY calls DESC;
```

### 3.2 적용 순서 (MP-127)

1. SSM 에 lol-server 자격증명 추가 (예: `/lol/prod/db-username-lol-server`, `/lol/prod/db-password-lol-server`). 비밀번호는 생성해서 SSM 에만 둔다.
2. 슈퍼유저로 `01-roles.sql` 실행 → `03-verify.sql` 통과 확인. psql 15 이상(`\getenv`).
   - 비밀번호는 환경변수 `LOL_SERVER_DB_PASSWORD` 로 넘긴다. 명령행(`-v`, `kubectl exec -- env X=…`)에 쓰면 실행 중 `ps` 에 보이므로, 파드 셸 안에서 `read -rs LOL_SERVER_DB_PASSWORD && export LOL_SERVER_DB_PASSWORD` 로 입력한다.
   - 스크립트는 비밀번호가 담긴 `ALTER ROLE` 이 서버 로그·`pg_stat_statements` 에 남지 않도록 트랜잭션 안에서 `log_statement`·`log_min_error_statement`·`track_utility` 를 끈다.
   - 환경변수가 없으면 아무것도 바꾸지 않고 0 이 아닌 종료 코드로 끝난다.
   - 이 시점엔 lol-server 가 아직 슈퍼유저로 접속 중이라 동작 변화 없음.
3. lol-deploy: `app/templates/external-secrets.yaml` 에 lol-server 키 추가, `lol-server.yaml` 의 `DB_USERNAME`/`DB_PASSWORD` 를 그 키로 교체. `lol-repository.yaml` 은 건드리지 않는다.
4. lol-server 롤아웃 (이미지는 그대로, 자격증명만 바뀐다).
5. 확인: lol-server 주요 API, `SELECT usename, application_name, count(*) FROM pg_stat_activity GROUP BY 1, 2;` 로 `lol_server` 접속 확인, lol-server 로그 `permission denied` 0건.
6. 롤백: lol-deploy 의 lol-server 자격증명 키를 슈퍼유저로 되돌리기만 하면 된다 (권한·search_path 는 슈퍼유저에 영향 없음).

01 이후 lol-db-schema 에 **app 테이블 마이그레이션**을 추가하지 않는다. 앱 테이블 변경은 lol-server app 체인(V3~)에 넣고 P2 이후 lol-server 배포로 적용한다 (1절).

## 4. P2 — app 스키마 이동 (MP-136)

### 4.1 사전 조건 (모두 충족해야 진행)

- [ ] P1 완료: lol-server 가 `lol_server` 계정으로 동작, `03-verify.sql` 통과
- [ ] lol-server 새 이미지(app Flyway 체인 + `individually` + `currentSchema=app,riot,public`) 빌드 완료, **아직 롤아웃하지 않음** — 이 이미지는 이동 전 DB 에서는 의도적으로 기동에 실패한다 (`create-schemas: false` + `db/migration-guard`)
- [ ] lol-db-schema 에 app 테이블 마이그레이션이 추가되지 않았음 (3.2 끝)
- [ ] 직전 pg_dump 백업 (데이터는 바뀌지 않지만 카탈로그 변경이므로)

lol-repository 쪽 준비는 없다 — Riot 테이블은 그대로이고 이동 중 잠그지도 않는다.

### 4.2 실행

1. app 테이블을 오래 잡는 트랜잭션이 없는 때를 고른다. lol-repository 의 백필·랭킹 계산은 Riot 테이블만 쓰므로 기다릴 필요 없다 (리허설: `match` 를 배타 잠금으로 쥔 동안에도 이동 성공).

   ```sql
   SELECT pid, usename, state, now() - xact_start AS xact_age, left(query, 80)
   FROM pg_stat_activity
   WHERE datname = current_database() AND state <> 'idle' AND pid <> pg_backend_pid()
   ORDER BY xact_age DESC NULLS LAST;
   ```
   app 테이블을 건드리는 수 초 넘는 트랜잭션이 있으면 끝날 때까지 기다린다.
2. `psql -v ON_ERROR_STOP=1 -f 02-move-schema.sql`
   - app 테이블 전부를 `LOCK TABLE` 한 문장으로 먼저 잡는다. 잠금 하나당 대기 2초(`lock_timeout`), 잠금 전체 대기 5초(`statement_timeout`)가 상한이다. 대기 중엔 그 테이블의 조회도 줄을 서므로 **lol-server 의 회원·커뮤니티·듀오 요청이 멈출 수 있는 최대 시간 ≈ 5초**. 전적·랭킹 등 Riot 테이블 조회는 막히지 않는다.
   - 잠금을 다 잡은 뒤의 이동은 리허설 기준 수십 ms.
   - 상한을 넘으면 `canceling statement due to lock timeout` / `statement timeout` 으로 아무것도 바뀌지 않은 채 롤백된다 → 1단계부터 다시.
3. 출력 끝의 스키마별 객체 수가 app 13 테이블·11 시퀀스(`lol_server`) / public 51 테이블·30 시퀀스·6 뷰(Riot 소유 계정)인지 확인하고 (공유 체인 V36 까지 적용돼 있었다면 app 14·12), `03-verify.sql` 이 P2 로 판별해 통과하는지 확인.
4. **lol-server 새 이미지 롤아웃** (4.1 의 이미지). 기동 로그에 `Successfully baselined schema with version: 1` → `Successfully applied 1 migration to schema "app", now at version v2` 가 찍히고 `Started LolServerApplication` 까지 가는지 확인. 실행 중이던 옛 파드는 커넥션 풀로 이동 후에도 동작하지만 재시작하면 `grouped` 검증으로 죽으므로 롤아웃을 미루지 않는다. 이후 `03-verify.sql` 을 다시 돌려 통과 확인 (app 14 테이블).
5. 확인: lol-server 주요 API(전적·랭킹·커뮤니티·듀오·커뮤니티 이미지 업로드), lol-server 로그의 `does not exist`·`permission denied` 0건. lol-repository 는 할 일이 없다 (재시작해도 그대로 기동함을 리허설에서 확인 — 부록).
6. 실행 기록: 시각, 소요 시간(`\timing` 출력), 재시도 횟수를 MP-136 에 남긴다.

### 4.3 롤백

1. lol-server 새 이미지를 이미 롤아웃했다면 **먼저 이전 이미지로 되돌린다** — 새 이미지는 app 테이블이 public 에 있으면 기동을 거부한다.
2. `psql -v ON_ERROR_STOP=1 -f 02-rollback.sql` → `03-verify.sql` (P1 으로 판별돼야 함). 데이터 미변경이라 즉시 끝난다. 잠금은 이동과 같은 방식(선점, 총 5초 상한)이며 시간 초과로 실패하면 변경 없이 롤백되니 다시 실행한다. app 테이블 소유자는 Riot 소유 계정으로 돌아가고, lol-server 가 V2 로 만든 `community_image` 도 public 으로 돌아가며, `app.flyway_schema_history` 는 삭제된다 (다시 이동하면 lol-server 가 baseline 부터 새로 쓰고 V2 는 `IF NOT EXISTS` 라 무해).

## 5. lol-repository 와의 경계

lol-server 는 Riot 테이블(매치·소환사·리그 등)을 아직 직접 읽는다. 그동안 lol-repository 가 계정·스키마·마이그레이션을 따로 바꾸더라도 아래만 지켜지면 lol-server 배포·설정 변경은 필요 없다.

- `lol_server` 가 읽는 Riot 테이블에 SELECT, 그 스키마에 USAGE 를 유지한다. lol-repository 가 계정을 바꾸면 `ALTER DEFAULT PRIVILEGES FOR ROLE <새 계정> IN SCHEMA <스키마> GRANT SELECT ON TABLES TO lol_server` 도 새로 건다 (`01` 의 기본 권한은 지금의 Riot 소유 계정 기준).
- Riot 테이블을 옮긴다면 스키마 이름은 `riot` — lol-server 의 search_path 와 데이터소스 URL(`currentSchema=app,riot,public`)에 이미 들어 있다. 다른 이름이면 lol-server 설정 변경이 필요하다.
- 같은 이름의 테이블을 두 스키마에 두지 않는다 (2절 1).
- `app` 스키마와 app 테이블은 건드리지 않는다. lol-db-schema 의 app 테이블 마이그레이션(V36 이후)은 lol-repository 에서 적용하지 않는다 (2절 4).

## 6. 다음 단계 (이 런북 범위 밖)

- MP-144 (lol-server): 코드는 완료 — app 체인·`AppSchemaMigrationTest`(Testcontainers). 운영 적용은 4.2 의 4단계.
- MP-155: lol-server 는 lol-db-schema 를 `rehearse.sh` 에서만 쓴다 (V1~V35 로 prod 같은 DB 를 만들 때). 서브모듈을 없애려면 리허설 시작 상태를 다른 방법으로 만든다.
- lol-server 의 Riot 읽기 의존을 없애면 5절 경계 조건과 `lol_server` 의 Riot SELECT 를 회수한다.
- lol-repository 의 계정·`riot` 스키마·자기 마이그레이션 체인(MP-143·MP-163)은 lol-repository 가 자기 일정으로 한다.

## 7. 로컬 DB

lol-server local 프로필은 기동하며 app 스키마를 직접 만든다 (`create-schemas` 기본값 true, 가드 없음).

- 빈 DB: lol-server 가 app 에 V1·V2 를 적용한다. Riot 테이블(매치·소환사 등)은 아직 lol-server 모듈이 읽으므로 lol-repository 를 한 번 띄워 마이그레이션해야 Hibernate 검증이 통과한다.
- 예전 DB(lol-repository 가 public 에 전부 만든 것): lol-server 가 app 에 새 테이블을 만든다. public 의 회원·게시글 등은 쓰이지 않는 사본으로 남는다 (search_path 상 app 이 먼저). 로컬 데이터를 이어 쓰려면 `INSERT INTO app.<t> SELECT * FROM public.<t>` 후 시퀀스를 맞춘다.
- lol-repository 는 공유 체인으로 public 에 app 테이블도 계속 만든다 — 그래서 "public 에 app 테이블이 있으면 거부" 가드는 prod 에만 둔다.

## 부록 — 로컬 리허설

```bash
docs/db-split/rehearse.sh          # Postgres 16 컨테이너(localhost:55432)에서 공유 V1~V35 → 01 → (app 체인 거부 확인) → 02 → lol-repository 무변화 확인 → app 체인 → 롤백 → 02 → app 체인, 매 단계 03 검증
KEEP=1 docs/db-split/rehearse.sh   # 끝난 뒤 P2 상태 DB 를 남김 (앱 기동 리허설용)
SHARED_TARGET=36 docs/db-split/rehearse.sh  # 공유 체인 V36 까지 적용된 경우 (V2 가 무해한지)
```

공유 체인은 이 레포의 `lol-db-schema` 서브모듈로 만든다. V1~V35 가 lol-repository 배포본(서브모듈 `d25aec8`)과 바이트 단위로 같아야 lol-repository 가 Flyway 체크섬 검증을 통과한다 — 지금 포인터 `2ad34f2`(lol-db-schema main)는 같고 V36 만 더 있다.

앱 기동 리허설은 위 DB 에 각 jar 를 붙여서 한다 (Redis·RabbitMQ 컨테이너 별도). lol-repository 는 prod 처럼 슈퍼유저로, lol-server 는 `lol_server` 로 붙는다. 비밀번호는 `KEEP=1` 실행 끝에 출력된 값을 export 한다:

```bash
# lol-repository (지금 배포본 그대로 — 추가 인자 없음)
SPRING_PROFILES_ACTIVE=local java -jar <lol-repository>/module/app/application/build/libs/application-*.jar \
  --spring.datasource.url=jdbc:postgresql://localhost:55432/postgres \
  --spring.datasource.username=postgres --spring.datasource.password="$POSTGRES_PASSWORD"
# lol-server (local seed 는 Riot 테이블 쓰기라 끈다). app 체인을 lol-server 가 직접 적용하는 걸 보려면
# 먼저 DROP TABLE app.flyway_schema_history, app.community_image; 로 "이동 직후 V35" 상태를 만든다.
SPRING_PROFILES_ACTIVE=local java -jar module/app/application/build/libs/application-*.jar \
  --spring.datasource.url='jdbc:postgresql://localhost:55432/postgres?currentSchema=app,riot,public' \
  --spring.datasource.username=lol_server --spring.datasource.password="$LOL_SERVER_DB_PASSWORD" \
  --spring.sql.init.mode=never
```

2026-10-20 리허설 (app 만 이동, Postgres 16, 공유 V1~V35 — `SHARED_TARGET=36` 도 통과):

| 확인 | 결과 |
|---|---|
| 다른 세션이 `member` 를 15초 잡음 | 2초에 `lock timeout`, 변경 없음 (P1 유지) |
| 다른 세션이 `match`(Riot)를 배타 잠금으로 쥔 채 이동 | 성공 — app 13 테이블·11 시퀀스 `lol_server`, public 51 테이블·30 시퀀스·6 뷰 `postgres` |
| 이동 후 공유 체인 재실행 (lol-repository 재기동과 같은 검증) | `Schema "public" is up to date` |
| 이동 후 lol-repository develop jar (MP-163 없음)를 슈퍼유저로 기동, 추가 인자 없음 | Flyway 31건 validate → up to date → Hibernate 검증 → `Started LolRepositoryApplication` |
| 이동 후 공유 체인 V36 적용 (2절 4) | `relation "member" does not exist` 로 실패·롤백, 이력 V35 유지 |
| app 체인 → 롤백 → 재이동 → app 체인 | baseline 1 → V2, 매 단계 `03-verify` 통과 |
| `01-roles.sql` 비밀번호 환경변수 없음 | 0 이 아닌 종료 코드, 변경 없음 |

2026-10-20 lol-server app 체인 리허설 (공유 V35 → 01 → 02 이동 → lol-server jar 를 `lol_server` 로 기동): `Successfully baselined schema with version: 1` → V2 적용 → 전체 엔티티 Hibernate 검증 통과 → `Started LolServerApplication`. 이동 전 DB 에서는 prod 설정이 아무것도 만들지 않고 실패한다 (`rehearse.sh`, `AppSchemaMigrationTest`).

2026-10-07 리허설 (이전 설계: app·Riot 동시 이동, `lol_repository` 계정 포함 — 참고용, Postgres 16, V1~V36):

| 단계 | lol-repository | lol-server |
|---|---|---|
| P1 (01 적용) | 기동, Flyway 32건 validate | 기동 (validate), Riot 쓰기 `permission denied`, 앱 RW·riot 읽기 정상 |
| 앱 기동 중 02 이동 (15~25ms) | 랭킹 계산 200, 백필 Job 메타 기록(`riot.batch_*`) | 전적·챔피언 랭크·타임라인·커뮤니티·듀오·시즌·랭킹·티어컷 200 |
| P2 재기동 | `default-schema` 미지정: **기동 실패**(2절 5) / 지정: 기동 | `grouped`: **기동 실패**(2절 1) / `individually`: 기동 |
| 롤백 → 재이동 | `03-verify` 통과 (P1 / P2) | 롤백 권한 소실 버그 수정 후 통과 |
