# lol-server app 스키마

lol-server 는 **app 스키마(회원·커뮤니티·듀오)만 쓴다**. app 밖 데이터는 DB 가 아니라 lol-repository API 로 받는다. 근거는 [서비스 분리 설계](https://linear.app/metapick/document/14e7fbfeb5bc) 6.1·8절, [ADR 서비스 역할과 책임 경계](https://linear.app/metapick/document/d7ffc5b22f69) 3.1절, Linear 프로젝트 「Riot 데이터 도메인 lol-repository 이전」.

운영도 **데이터를 옮기지 않고 app 스키마를 새로 시작한다**. public 의 예전 app 테이블을 app 으로 옮기던 스크립트(`02-move-schema.sql`·`02-rollback.sql`·`03-verify.sql`·`rehearse.sh`)와 "옮기기 전엔 기동 거부" 가드는 쓰지 않기로 해 지웠다 (git 이력 `105176f4`).

## 1. 원칙

- **app 테이블** = `member`, `member_withdrawal`, `social_account`, `community_*`, `duo_*` — 14개.
- **DDL**: lol-server 체인 `module/app/application/src/main/resources/db/migration/app` 만. V1 = 공유 체인(lol-db-schema) V35 시점의 app 테이블 13개 + 커뮤니티 기준 데이터, V2 = `community_image`. 이후 변경은 이 체인에 새 버전으로 추가하고, 적용된 버전은 주석도 고치지 않는다(체크섬). lol-db-schema 에는 넣지 않는다.
- **접속 계정**: 공유 슈퍼유저(lol-deploy `postgres-credentials`)로 접속하고 그대로 마이그레이션한다 — `SET ROLE` 이나 전용 계정을 전제하지 않는다 (`AppSchemaMigrationTest.prodMigratesAsConnectingAccount`). `app` 스키마와 그 안 객체는 그 계정 소유다.
- **app 밖 테이블에 엔티티를 만들지 않는다** — Hibernate `ddl-auto: validate` 는 현재 스키마(`currentSchema=app`)만 보므로 기동이 실패한다. 슈퍼유저라 DB 권한으로는 막히지 않으니 코드와 리뷰로 지킨다. app 밖 데이터는 lol-repository API 로 받는다.

## 2. 운영 적용

Flyway 설정은 local 과 prod 가 같다 (`postgresql-{local,prod}.yml`).

- **첫 롤아웃**: DB 수동 작업이 없다. Flyway 가 `app` 스키마를 만들고 V1 부터 적용한다 — 로그 `Creating schema "app"` → `Successfully applied 4 migrations to schema "app", now at version v4` → `Started LolServerApplication`.
- **예전 데이터**: public 의 회원·게시글·듀오는 옮기지 않으므로 새 앱에서 보이지 않는다 — 다시 가입·연동한다. lol-server 는 public 을 읽지 않으니(`currentSchema=app`) 예전 테이블은 백업으로 두었다가 지운다. 교체 중 옛 파드에 들어온 쓰기도 public 에 남는다.
- **이력 없이 테이블만 있는 app 스키마는 기동 실패**: baseline 하지 않는다 — V1 을 건너뛰고 빈 체인처럼 보이는 사고를 막는다 (`AppSchemaMigrationTest.refusesTablesWithoutHistory`). 손으로 만든 테이블이면 `DROP SCHEMA app CASCADE` 후 다시 띄운다.
- **롤백**: lol-deploy 에서 lol-server 이미지 태그를 되돌린다. 옛 이미지는 `app` 을 보지 않으므로 DB 는 그대로 둬도 된다 (다시 올리면 이미 적용된 버전은 건너뛴다). 직전 운영 이미지(main `4049e9c`)는 `RABBITMQ_*`·`LOL_REPOSITORY_URL` 이 있어야 기동하므로 lol-deploy #83(MP-167)도 함께 되돌린다.
- **롤아웃 시점**: 이 이미지에는 소환사·전적·랭킹·관전·game-data API 가 없다 (그 요청은 `401`). lol-ui 가 그 호출을 lol-repository 로 옮긴 릴리스와 같은 시간대에 내보낸다.

## 3. 전용 DB 계정 — 보류 (MP-127 · MP-162)

app 스키마만 소유하고 app 밖 권한이 없는 `lol_server` 계정으로 접속하게 하려던 단계다. 운영에 그 계정을 만들지 않기로 해 보류했다. 다시 하려면:

1. 계정 생성 — git 이력의 `docs/db-split/01-roles.sql` (`git show a4d6909d:docs/db-split/01-roles.sql`). 비밀번호는 SSM `/lol/prod/db-password-lol-server` 와 같은 값
2. `app` 스키마와 그 안 객체의 소유자를 `lol_server` 로 (`ALTER SCHEMA app OWNER TO lol_server`, 테이블·시퀀스 `OWNER TO`)
3. lol-deploy `lolServer.dbAccount.passwordSecret: lol-server-db-credentials` (lol-deploy #88 이 넣고 #90 이 뺀 줄)
4. 권한 검증(`git show a4d6909d:docs/db-split/03-verify.sql`)과 `REVOKE`·`search_path` 정리(MP-162)

## 4. 로컬 DB

- 빈 DB: lol-server 가 app 에 V1 부터 적용한다. lol-repository 없이 기동하고 동작한다 (듀오 글·신청은 통계 없이 만들어진다).
- 예전 DB(공유 체인이 public 에 전부 만든 것): lol-server 가 app 에 새 테이블을 만들고, public 의 회원·게시글 등은 쓰이지 않는 사본으로 남는다. 로컬 데이터를 이어 쓰려면 `INSERT INTO app.<t> SELECT * FROM public.<t>` 후 시퀀스를 맞춘다.
