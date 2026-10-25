# lol-server app 스키마 런북 (MP-136)

lol-server 는 **app 스키마(회원·커뮤니티·듀오)만 쓴다**. app 밖 데이터는 DB 가 아니라 lol-repository API 로 받는다. 이 런북도 app 만 다룬다 — 분류는 app 테이블 목록 하나이고, 그 밖의 객체는 이름을 몰라도 되게 짰다. 근거는 [서비스 분리 설계](https://linear.app/metapick/document/14e7fbfeb5bc) 6.1·8절, [ADR 서비스 역할과 책임 경계](https://linear.app/metapick/document/d7ffc5b22f69) 3.1절, Linear 프로젝트 「Riot 데이터 도메인 lol-repository 이전」.

lol-server 는 **공유 슈퍼유저 계정**(lol-deploy `postgres-credentials`)으로 접속한다. 전용 계정(`lol_server`)은 보류했다 — 5절.

| 파일 | 용도 | 실행 시점 |
|---|---|---|
| [`02-move-schema.sql`](02-move-schema.sql) | app 테이블만 `public` → `app` 이동, 단일 트랜잭션, app 테이블 잠금 선점 + 총 대기 5초 상한. 소유자는 바꾸지 않는다 | 4.2-3 |
| [`02-rollback.sql`](02-rollback.sql) | 역방향 이동 | 4.3 |
| [`03-verify.sql`](03-verify.sql) | app 테이블이 이동 전엔 전부 `public`, 이동 후엔 전부 `app` 에 있는지, `app` 에 다른 테이블이 없는지 검증, 위반 시 실패 (읽기 전용) | 매 단계 직후 |
| [`rehearse.sh`](rehearse.sh) | 로컬 Docker 로 전 과정 리허설 | 변경 시마다 |

모든 SQL 은 슈퍼유저(`POSTGRES_USER`)로 대상 DB 에서 실행한다. DB 작업은 lol-repository 배포·설정 변경을 요구하지 않는다. 다만 이 이미지에는 Riot 데이터 API 가 없으므로, 롤아웃 시점은 lol-ui 가 그 호출을 옮긴 뒤다 (4.1).

## 1. 원칙

- **app 테이블** = `member`, `member_withdrawal`, `social_account`, `community_*`, `duo_*` — 운영(V35) 13개, app 체인 V2 의 `community_image` 를 더해 14개. `02`·`02-rollback`·`03` 이 같은 술어를 쓴다. 새 app 테이블이 이 이름 규칙을 벗어나면 술어도 같이 고친다.
- **접속·소유**: lol-server 는 접속 계정(공유 슈퍼유저) 그대로 마이그레이션한다 — `SET ROLE` 이나 전용 계정을 전제하지 않는다 (`AppSchemaMigrationTest.migratesAsConnectingAccount`). `app` 스키마와 Flyway 가 만드는 객체는 그 계정 소유이고, 옮긴 테이블은 원래 소유자 그대로다.
- **DDL**: lol-server 체인 `module/app/application/src/main/resources/db/migration/app` 만. V1 = 운영(V35) app 테이블 13개 + 기준 데이터 그대로(pg_dump 비교로 확인), V2 = `community_image`(`IF NOT EXISTS`). 운영은 이동 직후 V1 을 baseline 으로 기록하고 V2 부터 적용한다. 이후 app 테이블 변경은 이 체인에 새 버전으로, 다시 적용해도 실패하지 않게(`IF NOT EXISTS` — 4.3 롤백 후 재이동) 추가하고, lol-db-schema 에는 넣지 않는다.

## 2. 함정

1. **app 밖 테이블에 엔티티를 만들면 기동 실패** — Hibernate `ddl-auto: validate` 는 현재 스키마(`currentSchema=app`)만 본다. app 밖을 직접 읽던 riot 모듈(gamedata·leaderboard·summoner·match)이 빠지면서 과도기 설정(`individually` + `currentSchema=app,public`)도 지웠다.
   → app 밖 데이터는 엔티티가 아니라 lol-repository API 로 받는다. 슈퍼유저라 DB 권한으로는 막히지 않으니 코드에서 지킨다.
2. **이동 후 공유 체인(lol-db-schema)이 V36 이후의 app 마이그레이션을 적용하면 실패** — V36(`community_image`)은 `member`·`community_post` 를 FK 로 참조하는데 그 테이블이 `public` 에 없다 (`relation "member" does not exist`, 리허설에서 재현). lol-repository 는 자기 `riot` 체인으로 옮겼다.
3. **`02` 직후 옛 이미지는 회원·커뮤니티·듀오가 500** — 옛 이미지는 search_path 의 `public` 에서 찾는다. 떠 있는 파드도 커넥션 풀로 버티지 않는다 (리허설에서 재현). → 새 파드를 미리 띄워 두고 `02` 뒤 곧바로 재기동한다 (4.2).

## 3. 단계

| 단계 | 내용 | 언제 |
|---|---|---|
| **A. app 스키마 이동** (MP-136) | `02` → develop 이미지 롤아웃 (riot 모듈 없음, app 체인 V4 까지) | lol-ui 가 Riot 데이터 호출을 lol-server 에서 뗀 뒤 (4.1). develop 은 MP-144 이후 A 없이는 prod 에 배포할 수 없다 (가드) |

## 4. A — app 스키마 이동 (MP-136)

### 4.1 사전 조건

- [ ] **lol-ui 운영이 lol-server 의 Riot 데이터 경로를 더 이상 부르지 않는다.** develop 이미지에는 소환사·전적·랭킹·관전·game-data API(운영 main 기준 GET 26개)가 없다. 그대로 올리면 그 요청은 `401`(인증 필요로 막힘 — 404 가 아니다)이고, lol-ui 는 401 에 토큰 갱신을 시도하다 실패하면 로그인 페이지로 보낸다. 필요한 것: lol-repository 운영 이미지의 `/api/v1` 공개 API, Ingress 경로(MP-151·153·154), lol-ui 전환(MP-170·172~175). champion-stats·회원·인증·커뮤니티·듀오는 옛 경로도 lol-server 가 그대로 받는다
- [ ] lol-deploy 가 lol-server 에 공유 슈퍼유저 자격증명을 준다 (`lolServer.dbAccount.passwordSecret` 이 비어 있음 — lol-deploy #90)
- [ ] 릴리스 PR(develop → main). main 머지가 곧 롤아웃이므로 **점검 창을 시작하며 머지한다** — 새 이미지는 이동 전 DB 에서 의도적으로 기동에 실패하고(`create-schemas: false` + `db/migration-guard`), `maxUnavailable: 0` 이라 그동안 옛 파드가 계속 서빙한다. 새 파드가 가드에 걸려 재시작을 기다리는 상태가 2단계의 출발점이다
- [ ] 롤백 준비: 직전 이미지(main `4049e9c`)는 `RABBITMQ_*`·`LOL_REPOSITORY_URL` 이 있어야 기동하는데 lol-deploy #83(MP-167)이 뺐다 — 롤백은 이미지 태그와 #83 을 함께 되돌린다 (4.3)
- [ ] 직전 pg_dump 백업 (데이터는 바뀌지 않지만 카탈로그 변경이므로)

### 4.2 실행

1. `03-verify.sql` → `phase = P1 (이동 전)`, `검증 통과`.
2. app 테이블을 오래 잡는 트랜잭션이 없는 때를 고른다. app 밖 작업(배치 등)은 기다릴 필요 없다 — 이동은 app 테이블만 잠근다. **`02` 가 커밋되는 순간부터 새 파드가 Ready 가 될 때까지 옛 파드의 회원·커뮤니티·듀오 API 는 500 이다** (2절 3).

   ```sql
   SELECT pid, usename, state, now() - xact_start AS xact_age, left(query, 80)
   FROM pg_stat_activity
   WHERE datname = current_database() AND state <> 'idle' AND pid <> pg_backend_pid()
   ORDER BY xact_age DESC NULLS LAST;
   ```
3. `psql -v ON_ERROR_STOP=1 -f 02-move-schema.sql`
   - app 테이블 전부를 `LOCK TABLE` 한 문장으로 먼저 잡는다. 잠금 하나당 대기 2초(`lock_timeout`), 전체 대기 5초(`statement_timeout`)가 상한이다. 대기 중엔 그 테이블 조회도 줄을 서므로 **잠금 대기로 회원·커뮤니티·듀오 요청이 멈추는 시간 ≈ 최대 5초** (그 뒤 새 파드가 뜰 때까지의 500 은 2단계).
   - 잠금을 다 잡은 뒤의 이동은 수십 ms. 상한을 넘으면 `lock timeout` / `statement timeout` 으로 아무것도 바뀌지 않은 채 롤백된다 → 2단계부터 다시.
4. 출력이 `moved tables: app=13`, app 13 테이블·11 시퀀스(소유자는 이동 전 그대로)인지, `03-verify.sql` 이 P2 로 판별해 통과하는지 확인.
5. **곧바로 새 파드를 다시 띄운다** — 가드에 걸려 재시작을 기다리는 새 파드를 `kubectl -n app delete pod <새 파드>` 로 지우면 CrashLoopBackOff 대기 없이 바로 뜬다. 500 공백 ≈ 새 파드 기동(로컬 약 5초) + startupProbe 주기(10초). 기동 로그에 `Successfully baselined schema with version: 1` → `Successfully applied 3 migrations to schema "app", now at version v4` → `Started LolServerApplication`. 이후 `03-verify.sql` 을 다시 돌려 통과 확인 (app 14 테이블).
6. 확인: 회원·커뮤니티·듀오·커뮤니티 이미지 업로드, 로그의 `does not exist` 는 Flyway 의 `Schema history table ... does not exist yet` 안내 한 줄뿐. 이동 전에 RSO 를 연동한 회원은 듀오에서 `RIOT_ACCOUNT_RELINK_REQUIRED` 가 정상이다 — 연동에 Riot ID 가 없다(app V4). 연동을 해제했다가 다시 하면 풀린다.
7. 실행 기록: 시각, 소요 시간(`\timing` 출력), 재시도 횟수를 MP-136 에 남긴다.

### 4.3 롤백

1. 새 이미지를 롤아웃했다면 **먼저 이전 이미지로 되돌린다** — 새 이미지는 app 테이블이 public 에 있으면 기동을 거부한다. 이전 이미지(main `4049e9c`)는 `RABBITMQ_*`·`LOL_REPOSITORY_URL` 이 있어야 뜨므로 lol-deploy 에서 `lolServer.image.tag` 와 #83(MP-167)을 함께 되돌린다.
2. `psql -v ON_ERROR_STOP=1 -f 02-rollback.sql` → `03-verify.sql` (P1 으로 판별돼야 함). 잠금은 이동과 같은 방식이며 시간 초과로 실패하면 변경 없이 롤백되니 다시 실행한다. V2 의 `community_image`·V3·V4 의 컬럼도 테이블째 public 으로 돌아가고, `app.flyway_schema_history` 는 삭제된다. 다시 이동하면 baseline 부터 새로 쓰고 V2~V4 를 다시 적용하는데, 모두 `IF NOT EXISTS` 라 무해하다 (V2 이후 마이그레이션은 이 경로 때문에 멱등이어야 한다 — `AppSchemaMigrationTest`).

## 5. 전용 DB 계정 — 보류 (MP-127 · MP-162)

app 스키마만 소유하고 app 밖 권한이 없는 `lol_server` 계정으로 접속하게 하려던 단계다. 운영에 그 계정을 만들지 않기로 해 보류했고, lol-server 는 `lol_server` 역할에 기대지 않는다. 다시 하려면:

1. 계정 생성 스크립트 — git 이력의 `docs/db-split/01-roles.sql` (`git show a4d6909d:docs/db-split/01-roles.sql`). 비밀번호는 SSM `/lol/prod/db-password-lol-server` 와 같은 값
2. app 스키마와 그 안의 객체 소유자를 `lol_server` 로 (`ALTER SCHEMA app OWNER TO lol_server`, 테이블·시퀀스 `OWNER TO`)
3. lol-deploy `lolServer.dbAccount.passwordSecret: lol-server-db-credentials` (lol-deploy #88 이 넣고 #90 이 뺀 두 줄)
4. `03-verify.sql` 에 권한 검사 되살리기 (`a4d6909d` 판), `REVOKE`·`search_path` 정리(MP-162)

## 6. 로컬 DB

local 프로필은 슈퍼유저로 접속하고 기동하며 app 스키마를 직접 만든다 (`create-schemas` 기본값 true, 가드 없음).

- 빈 DB: lol-server 가 app 에 V1 부터 적용한다. lol-repository 없이 기동하고 동작한다 (듀오 글·신청은 통계 없이 만들어진다).
- 예전 DB(lol-repository 가 public 에 전부 만든 것): lol-server 가 app 에 새 테이블을 만든다. public 의 회원·게시글 등은 쓰이지 않는 사본으로 남는다 (search_path 상 app 이 먼저). 로컬 데이터를 이어 쓰려면 `INSERT INTO app.<t> SELECT * FROM public.<t>` 후 시퀀스를 맞춘다.
- lol-repository 가 공유 체인으로 public 에 app 테이블까지 만드는 동안은 로컬에서 항상 걸리므로 "public 에 app 테이블이 있으면 거부" 가드는 prod 에만 둔다.

## 부록 — 로컬 리허설

```bash
docs/db-split/rehearse.sh          # Postgres 16 컨테이너(localhost:55432)에서 이동 전 상태 → (새 이미지 거부 확인) → 잠금 경합 → 02 → 롤아웃 → 롤백 → 02 → 롤아웃, 매 단계 03 검증
KEEP=1 docs/db-split/rehearse.sh   # 끝난 뒤 DB 를 남김
```

이동 전 상태는 app 체인 V1 을 `public` 에 적용해 만든다 (운영 V35 의 app 테이블과 같다 — 1절). app 밖 객체는 이름 없는 테이블 하나(`other_service`)로 대신해, 이동이 그 테이블을 잠그지 않는지를 본다.

2026-10-26 (Postgres 16, app 체인 V1~V4, `lol_server` 역할 없음):

| 확인 | 결과 |
|---|---|
| `rehearse.sh` 전 과정 (거부 → 잠금 경합 → 이동 → 롤아웃 → 롤백 → 재이동 → 롤아웃) | 매 단계 `03` 통과. 이동은 app 13 테이블·11 시퀀스(소유자 `postgres` 그대로), 롤아웃은 baseline 1 → V2~V4 |
| 빈 `app` 스키마만 있고 테이블은 `public` 에 있는 상태에서 `03` | 실패 — "app 에 app 테이블이 없음", "app 테이블이 app 밖에 있음" |
| jar, 기존 데이터가 있는 이동 전 DB → `02`, **prod 프로필 + 슈퍼유저 접속**, lol-repository 없음 | baseline 1 → `now at version v4` → `Started LolServerApplication in 5 seconds`, app 14 테이블·이력 소유자 `postgres`, `03` 통과, 커뮤니티·듀오·회원 경로 200 |

이전 기록 (2026-10-20·21) — 당시엔 `lol_server` 역할(`01-roles.sql`)과 Flyway `SET ROLE lol_server` 를 전제했다. 잠금·롤백·가드·500 공백 결과는 지금도 같다:

| 확인 | 결과 |
|---|---|
| 다른 세션이 `member` 를 15초 잡음 | 2~3초에 `lock timeout`, 변경 없음 (P1 유지) |
| app 밖 테이블을 배타 잠금으로 쥔 채 이동 | 성공 — app 13 테이블·11 시퀀스 |
| 이동 후 공유 체인 V36 적용 (2절 2) | `relation "member" does not exist` 로 실패·롤백 |
| 떠 있는 운영 main(`4049e9c`) jar 에서 `02` | 이동 직후부터 회원·커뮤니티·듀오 500(`relation ... does not exist`) — 커넥션 풀로 버티지 않는다. 재시작하면 `missing table` 로 기동 실패 |
| `rehearse.sh` 의 롤백 → 재이동 → 롤아웃 (V3·V4 멱등화 전) | V3 `ADD COLUMN` 이 이미 있는 컬럼에 다시 적용돼 실패 → `IF NOT EXISTS` 로 고침 |
| lol-ui 운영의 Riot 데이터 경로를 develop jar 에 | 401 — 4.1 의 lol-ui 전환 없이 올리면 이렇게 된다 |
