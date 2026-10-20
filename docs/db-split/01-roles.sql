-- MP-127 / MP-136 lol-server 전용 DB 계정 (app 스키마 소유자)
--
-- 실행: 슈퍼유저(POSTGRES_USER)로, 대상 DB 에서. 몇 번을 다시 돌려도 결과가 같다 (멱등). psql 15 이상 필요(\getenv).
-- 비밀번호는 명령행(-v)이 아니라 환경변수로 넘긴다 — 명령행 인자는 실행 중 ps 에 보인다.
--   read -rs LOL_SERVER_DB_PASSWORD && export LOL_SERVER_DB_PASSWORD
--   psql -v ON_ERROR_STOP=1 -f 01-roles.sql
-- 비밀번호가 담긴 ALTER ROLE 문이 서버 로그·pg_stat_statements 에 남지 않도록 트랜잭션 안에서 기록을 끈다.
--
-- lol_server 는 app 스키마만 소유하고 그 밖의 객체에는 권한이 없다. app 밖 데이터는 lol-repository API 로 받는다.
-- 이 스크립트는 계정만 만든다 — app 테이블 소유권은 02-move-schema.sql 이 넘긴다.
-- lol-server 는 app 밖 테이블을 직접 읽는 모듈이 남아 있는 동안 지금 계정을 그대로 쓰고, 그 모듈이 모두 빠진 뒤
-- 이 계정으로 전환한다 (런북 5절). 그 전에도 lol-server Flyway 는 SET ROLE lol_server 로 돌아 app 객체는 이 계정 소유가 된다.

\set ON_ERROR_STOP on

-- 변수가 없으면 오류로 끝낸다 (\quit 은 종료 코드 0 이라 자동화가 성공으로 오인한다)
\getenv lol_server_password LOL_SERVER_DB_PASSWORD
\if :{?lol_server_password}
\else
  DO $$ BEGIN RAISE EXCEPTION 'LOL_SERVER_DB_PASSWORD 환경변수가 필요합니다'; END $$;
\endif

SELECT current_database() AS dbname \gset

BEGIN;

-- 비밀번호 평문이 담긴 문장을 기록하지 않는다 (이 트랜잭션 한정)
SET LOCAL log_statement = 'none';
SET LOCAL log_min_error_statement = 'panic';
SET LOCAL log_min_duration_statement = -1;
SET LOCAL pg_stat_statements.track_utility = off;   -- 확장 미로드여도 무해 (placeholder)

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'lol_server') THEN
        CREATE ROLE lol_server LOGIN;
    END IF;
END
$$;

ALTER ROLE lol_server WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'lol_server_password';
GRANT CONNECT ON DATABASE :"dbname" TO lol_server;

-- app 은 아직 없을 수 있다. 없는 스키마는 search_path 에서 건너뛴다.
ALTER ROLE lol_server IN DATABASE :"dbname" SET search_path = app;

-- app 밖에는 아무 권한도 두지 않는다 (재실행 시 잔여 권한 정리)
REVOKE CREATE ON SCHEMA public FROM lol_server;
REVOKE ALL ON ALL TABLES    IN SCHEMA public FROM lol_server;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM lol_server;

COMMIT;

\echo '--- 계정 · search_path'
SELECT r.rolname, r.rolcanlogin, r.rolsuper, s.setconfig
FROM pg_roles r
LEFT JOIN pg_db_role_setting s ON s.setrole = r.oid AND s.setdatabase = (SELECT oid FROM pg_database WHERE datname = current_database())
WHERE r.rolname = 'lol_server';
