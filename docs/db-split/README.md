# DB 계정·스키마 분리 런북 (MP-120 · MP-127 · MP-128 · MP-136)

Linear 프로젝트 「DB 계정·스키마 분리」의 P1(서비스별 계정)·P2(스키마 이동) 실행 자료. 근거는 [서비스 분리 설계](https://linear.app/metapick/document/14e7fbfeb5bc) 6.1·8절, [ADR 서비스 역할과 책임 경계](https://linear.app/metapick/document/d7ffc5b22f69) 3.1절.

| 파일 | 용도 | 실행 시점 |
|---|---|---|
| [`01-roles.sql`](01-roles.sql) | `lol_repository`·`lol_server` 계정, 계정별 search_path, public 테이블 단위 권한 (멱등) | P1 (MP-120 작성 / MP-127 적용) |
| [`02-move-schema.sql`](02-move-schema.sql) | `public` → `app`·`riot` 이동 (테이블·독립 시퀀스·뷰), 단일 트랜잭션 + `lock_timeout` | P2 (MP-136) |
| [`02-rollback.sql`](02-rollback.sql) | 역방향 이동 + 권한 복구 | P2 롤백 |
| [`03-verify.sql`](03-verify.sql) | 현재 단계(P1/P2)를 판별해 계정·권한·위치 검증, 위반 시 실패 (읽기 전용) | 매 단계 직후 |
| [`rehearse.sh`](rehearse.sh) | 로컬 Docker 로 전 과정 리허설 | 변경 시마다 |

모든 SQL 은 슈퍼유저(현재 `POSTGRES_USER`)로 대상 DB 에서 실행한다.

## 1. 분류와 소유권

ADR 3.1 표 기준. 분류 규칙은 네 SQL 파일에 같은 술어로 들어 있다.

| 분류 | 테이블 | 스키마 / 소유자 | `lol_server` | `lol_repository` |
|---|---|---|---|---|
| app (14) | `member`, `member_withdrawal`, `social_account`, `community_*`(9), `duo_*`(2) | `app` / `lol_server` | 소유 (RW·DDL) | P1: 소유(전환기) → P2: **권한 없음** |
| riot (50) | 매치·타임라인 이벤트·소환사·리그·랭킹(+`_backup`)·메타데이터·`item_meta`·e스포츠(15)·`batch_*`(6) | `riot` / `lol_repository` | SELECT 만 (P7 에서 회수) | 소유 |
| riot 독립 시퀀스 | `batch_job_seq`, `batch_job_execution_seq`, `batch_step_execution_seq` | `riot` / `lol_repository` | — | 소유 |
| riot 뷰 (6) | `v_skill_events_flat`, `v_item_events_flat`, `v_esports_*`(4) | `riot` / `lol_repository` | SELECT | 소유 |
| 이력 | `flyway_schema_history` (V1~V36) | `public` 유지 | 없음 | 소유 |
| 확장 | `btree_gist` | `public` 유지 | — | — |

V1~V36 기준 테이블 64개(+history), 시퀀스 42개(컬럼 소속 39 + 독립 3), 뷰 6개. 새 테이블이 `member`·`social_account`·`community_*`·`duo_*` 이름 규칙을 벗어나면 riot 으로 분류되므로 분류 규칙을 같이 고칠 것.

**전환기 결정 — P1 에서 앱 테이블도 `lol_repository` 소유**: P3(MP-144)까지는 lol-repository 의 Flyway 가 lol-db-schema 전체(앱 테이블 포함)를 적용하는 유일한 주체다. 앱 테이블 소유자를 P1 에서 `lol_server` 로 바꾸면 그 사이 앱 테이블 마이그레이션이 실패한다. 그래서 앱 테이블 소유권은 P2 이동 때 `lol_server` 로 넘기고, 그 시점부터 lol-db-schema 를 동결한다.

## 2. 리허설에서 드러난 함정 (설계 문서 6.1 보완)

설계 문서의 "search_path 를 계정 단위로 지정하면 코드 수정 없이 동작" 전제는 SQL 실행에는 맞지만, **기동 시 스키마 도구 두 곳에서 깨진다**. 둘 다 P2 전에 배포해야 한다.

1. **lol-repository Flyway 가 `riot` 에 history 를 새로 만들고 V2 부터 재적용 → 기동 실패**
   `default-schema` 미지정 시 Flyway 는 연결의 현재 스키마(search_path 첫 항목 = `riot`)에서 history 를 찾는다. 없으니 `baseline-on-migrate: true` 로 `riot.flyway_schema_history` 를 version 1 로 만들고 V2(COMMENT)를 적용한 뒤 V3 에서 `relation "idx_summoner_ranking_platform_queue_tier" already exists` 로 죽는다.
   → **lol-repository `persistence-{local,dev,prod}.yml` 에 `spring.flyway.default-schema: public` 추가** (P1 에서도 무해: 지금도 history 는 public). P3(MP-143)에서 `riot` 으로 바꾸고 baseline.
2. **lol-server Hibernate `ddl-auto: validate` 가 riot 테이블을 못 찾음 → 기동 실패**
   기본 메타데이터 추출 전략(`grouped`)은 현재 스키마(`app`) 하나만 조회 → `Schema-validation: missing table [building_events]`.
   → **lol-server `postgresql-{local,prod}.yml` 에 `hibernate.hbm2ddl.jdbc_metadata_extraction_strategy: individually`** (이 브랜치에 반영). 테이블마다 search_path 전체에서 찾는다. P1·P2 모두에서 기동 확인.
3. **롤백 시 lol_server 의 앱 테이블 쓰기 권한 소실** — 소유자를 `lol_server` → `lol_repository` 로 되돌리면 `lol_server` 앞 GRANT 가 사라진다 (`permission denied for table duo_post`). `02-rollback.sql` 이 권한을 다시 준다.
4. (로컬 전용) lol-server local 프로필의 `db/seed/season-local.sql` 은 Riot 테이블(`season`·`patch_version`)에 쓴다. 로컬에서 `lol_server` 계정으로 띄우면 실패하므로 로컬은 계속 슈퍼유저를 쓰거나 `spring.sql.init.mode=never`.

Spring Batch(`BATCH_*` 테이블·`BATCH_JOB_SEQ`), native query(lol-server match·timeline JSONB, lol-repository 랭킹 `_backup`·백필 집계 SQL), JPA·QueryDSL 은 search_path 로 코드 수정 없이 동작했다. 기존 커넥션 풀도 이동 직후 그대로 동작한다 (앱을 띄운 채 이동 리허설).

## 3. P1 — 서비스별 계정 (MP-120 → MP-127)

### 3.1 lol-server 의 Riot 테이블 쓰기 0건 확인 (MP-120)

- **정적 분석**: lol-server 운영 코드에 Riot 테이블 쓰기 경로 없음. 유일한 후보 `SummonerPersistenceAdapter.save` 는 호출자 없음(죽은 코드). `@Modifying`·native DML 없음. 비-readOnly `@Transactional` 은 `SummonerService` 1곳이며 Redis·MQ 만 건드린다. 로컬 시드 SQL 만 쓴다 (2절 4).
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

1. SSM 에 서비스별 자격증명 추가 (예: `/lol/prod/db-username-lol-server`, `/lol/prod/db-password-lol-server`, `…-lol-repository`). 비밀번호는 생성해서 SSM 에만 둔다.
2. 슈퍼유저로 `01-roles.sql` 실행 (비밀번호는 `-v` 변수로 전달, 셸 이력에 남기지 말 것) → `03-verify.sql` 통과 확인.
   - 이 시점엔 앱이 아직 슈퍼유저로 접속 중이라 동작 변화 없음.
3. lol-deploy: `app/templates/external-secrets.yaml` 에 서비스별 키 추가, `lol-server.yaml`·`lol-repository.yaml` 의 `DB_USERNAME`/`DB_PASSWORD` 를 각자 키로 교체. `POSTGRES_USER` 는 infra(Postgres StatefulSet)에서만 쓴다.
4. lol-repository 먼저 롤아웃 → Flyway validate·배치 정상 → lol-server 롤아웃.
5. 확인: 두 서비스 주요 API, `SELECT usename, application_name, count(*) FROM pg_stat_activity GROUP BY 1, 2;` 로 각 계정 접속 확인, lol-server 로그 `permission denied` 0건.
6. 롤백: lol-deploy 자격증명 키를 슈퍼유저로 되돌리기만 하면 된다 (권한·search_path 는 슈퍼유저에 영향 없음).

01 이후 lol-db-schema 에 **새 앱 테이블**이 추가되면 `lol_server` 에는 SELECT 만 열린다 (기본 권한). 배포 직후 `01-roles.sql` 을 다시 돌려 RW 를 준다.

## 4. P2 — 스키마 이동 (MP-136)

### 4.1 사전 조건 (모두 충족해야 진행)

- [ ] P1 완료: 두 서비스가 각자 계정으로 동작, `03-verify.sql` 통과
- [ ] lol-repository 에 `spring.flyway.default-schema: public` 배포됨 (2절 1)
- [ ] lol-server 에 `jdbc_metadata_extraction_strategy: individually` 배포됨 (2절 2)
- [ ] lol-db-schema 동결 선언 — 이동 후 MP-143·MP-144 완료 전까지 새 마이그레이션 금지 (Flyway 가 `default-schema=public` 으로 돌면 새 객체가 public 에 생긴다)
- [ ] 직전 pg_dump 백업 (데이터는 바뀌지 않지만 카탈로그 변경이므로)

### 4.2 실행

1. 오래 도는 작업 확인 — 백필 실행 중이 아니고(`GET /internal/backfill/{type}/runs`), `SummonerRankingScheduler`(2시간 주기) 실행 중이 아닌 때를 고른다:

   ```sql
   SELECT pid, usename, state, now() - xact_start AS xact_age, left(query, 80)
   FROM pg_stat_activity
   WHERE datname = current_database() AND state <> 'idle' AND pid <> pg_backend_pid()
   ORDER BY xact_age DESC NULLS LAST;
   ```
   수 초 넘는 트랜잭션이 있으면 끝날 때까지 기다린다. `MatchBatchProcessor`(1초 주기)의 짧은 쓰기는 멈출 필요 없다.
2. `psql -v ON_ERROR_STOP=1 -f 02-move-schema.sql` — 리허설 기준 트랜잭션 본문 약 25ms. `lock_timeout`(3초) 초과로 실패하면 전부 롤백되므로 1단계부터 재시도.
3. `03-verify.sql` 통과 확인. 출력의 스키마별 객체 수: app 14 테이블 / riot 50 테이블·30 시퀀스·6 뷰 / public 1 테이블.
4. 확인: lol-server 주요 API(전적·랭킹·커뮤니티·듀오), lol-repository 랭킹 계산·백필 1회(`BATCH_*` 시퀀스 포함), 두 서비스 로그의 `does not exist`·`permission denied` 0건. 두 서비스를 한 번씩 재시작해 기동 시 검증(Flyway validate, Hibernate validate)도 확인한다.
5. 실행 기록: 시각, 소요 시간(`\timing` 출력), 재시도 횟수를 MP-136 에 남긴다.

### 4.3 롤백

`psql -v ON_ERROR_STOP=1 -f 02-rollback.sql` → `03-verify.sql` (P1 으로 판별돼야 함). 데이터 미변경이라 즉시 끝난다. 앱 설정(2절 1·2)은 P1 에서도 무해하므로 되돌리지 않는다.

## 5. 다음 단계 (P3, 이 런북 범위 밖)

- MP-143 (lol-repository): `db/migration/riot`, `spring.flyway.default-schema: riot` + baseline. 기존 `public.flyway_schema_history` 는 이력으로 보존.
- MP-144 (lol-server): Flyway 활성화, `db/migration/app`, `app.flyway_schema_history` baseline. 현재 lol-server 테스트는 H2 `create-drop` 이라 Testcontainers 도입이 선행돼야 한다.
- 정리: `lol_repository` 의 public `CREATE` 회수, 동결 해제.

## 부록 — 로컬 리허설

```bash
docs/db-split/rehearse.sh          # Postgres 16 컨테이너(localhost:55432)에서 V1~Vn → 01 → 02 → 롤백 → 02, 매 단계 03 검증
KEEP=1 docs/db-split/rehearse.sh   # 끝난 뒤 P2 상태 DB 를 남김 (앱 기동 리허설용)
```

앱 기동 리허설은 위 DB 에 각 jar 를 서비스 계정으로 붙여서 한다 (Redis·RabbitMQ 컨테이너 별도):

```bash
# lol-repository (P2 에서는 default-schema 지정 필수)
SPRING_PROFILES_ACTIVE=local java -jar <lol-repository>/module/app/application/build/libs/application-*.jar \
  --spring.datasource.url=jdbc:postgresql://localhost:55432/postgres \
  --spring.datasource.username=lol_repository --spring.datasource.password=repopw \
  --spring.flyway.default-schema=public
# lol-server (local seed 는 Riot 테이블 쓰기라 끈다)
SPRING_PROFILES_ACTIVE=local java -jar module/app/application/build/libs/application-*.jar \
  --spring.datasource.url=jdbc:postgresql://localhost:55432/postgres \
  --spring.datasource.username=lol_server --spring.datasource.password=srvpw \
  --spring.sql.init.mode=never
```

2026-10-07 리허설 결과 (Postgres 16, V1~V36):

| 단계 | lol-repository | lol-server |
|---|---|---|
| P1 (01 적용) | 기동, Flyway 32건 validate | 기동 (validate), Riot 쓰기 `permission denied`, 앱 RW·riot 읽기 정상 |
| 앱 기동 중 02 이동 (21~25ms) | 랭킹 계산 200, 백필 Job 메타 기록(`riot.batch_*`) | 전적·챔피언 랭크·타임라인·커뮤니티·듀오·시즌·랭킹·티어컷 200 |
| P2 재기동 | `default-schema` 미지정: **기동 실패**(2절 1) / 지정: 기동 | `grouped`: **기동 실패**(2절 2) / `individually`: 기동 |
| 롤백 → 재이동 | `03-verify` 통과 (P1 / P2) | 롤백 권한 소실 버그 수정 후 통과 |
