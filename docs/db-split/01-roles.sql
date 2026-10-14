-- MP-120 (P1) 서비스별 DB 계정 · 권한 · search_path
--
-- 실행: 슈퍼유저(POSTGRES_USER)로, 대상 DB 에서. 몇 번을 다시 돌려도 결과가 같다 (멱등). psql 15 이상 필요(\getenv).
-- 비밀번호는 명령행(-v)이 아니라 환경변수로 넘긴다 — 명령행 인자는 실행 중 ps 에 보인다.
--   read -rs LOL_SERVER_DB_PASSWORD && export LOL_SERVER_DB_PASSWORD
--   read -rs LOL_REPOSITORY_DB_PASSWORD && export LOL_REPOSITORY_DB_PASSWORD
--   psql -v ON_ERROR_STOP=1 -f 01-roles.sql
-- 비밀번호가 담긴 ALTER ROLE 문이 서버 로그·pg_stat_statements 에 남지 않도록 트랜잭션 안에서 기록을 끈다.
--
-- 테이블은 아직 public 에 있다 (스키마 이동 전). 따라서 권한은 public 테이블 단위로 준다.
--   lol_repository : public 전 객체 소유 (전환기 동안 유일한 Flyway 실행 주체 — 앱 테이블 마이그레이션 포함)
--   lol_server     : 앱 테이블(회원·커뮤니티·듀오) RW, 그 외(Riot 데이터·배치 메타) SELECT 만
-- 앱 테이블 소유권은 스키마 이동(02-move-schema.sql) 때 lol_server 로 넘어간다.
--
-- 앱/riot 분류 규칙은 ADR 3.1 소유권 표 기준이며 02·03 스크립트와 동일하다:
--   app  = member, member_withdrawal, social_account, community_*, duo_*
--   riot = 그 외 전부 (flyway_schema_history 는 이력으로 public 에 남음)

\set ON_ERROR_STOP on

-- 변수가 없으면 오류로 끝낸다 (\quit 은 종료 코드 0 이라 자동화가 성공으로 오인한다)
\getenv lol_server_password LOL_SERVER_DB_PASSWORD
\getenv lol_repository_password LOL_REPOSITORY_DB_PASSWORD
\if :{?lol_server_password}
\else
  DO $$ BEGIN RAISE EXCEPTION 'LOL_SERVER_DB_PASSWORD 환경변수가 필요합니다'; END $$;
\endif
\if :{?lol_repository_password}
\else
  DO $$ BEGIN RAISE EXCEPTION 'LOL_REPOSITORY_DB_PASSWORD 환경변수가 필요합니다'; END $$;
\endif

SELECT current_database() AS dbname \gset

BEGIN;

-- 비밀번호 평문이 담긴 문장을 기록하지 않는다 (이 트랜잭션 한정)
SET LOCAL log_statement = 'none';
SET LOCAL log_min_error_statement = 'panic';
SET LOCAL log_min_duration_statement = -1;
SET LOCAL pg_stat_statements.track_utility = off;   -- 확장 미로드여도 무해 (placeholder)

-- 1. 계정 -----------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'lol_repository') THEN
        CREATE ROLE lol_repository LOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'lol_server') THEN
        CREATE ROLE lol_server LOGIN;
    END IF;
END
$$;

ALTER ROLE lol_repository WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'lol_repository_password';
ALTER ROLE lol_server     WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'lol_server_password';

GRANT CONNECT, TEMPORARY ON DATABASE :"dbname" TO lol_repository, lol_server;

-- 2. search_path (계정 단위) ---------------------------------------------
-- 스키마 riot·app 은 아직 없다. 없는 스키마는 search_path 에서 건너뛰므로 지금은 동작 변화 없음.
-- public 은 btree_gist 확장과 public.flyway_schema_history 때문에 유지.
ALTER ROLE lol_repository IN DATABASE :"dbname" SET search_path = riot, public;
ALTER ROLE lol_server     IN DATABASE :"dbname" SET search_path = app, riot, public;

-- 3. public 스키마 --------------------------------------------------------
-- lol_repository 는 Flyway 로 public 에 새 테이블을 만들 수 있어야 한다 (lol-db-schema 동결 전까지).
GRANT USAGE, CREATE ON SCHEMA public TO lol_repository;
GRANT USAGE ON SCHEMA public TO lol_server;
REVOKE CREATE ON SCHEMA public FROM lol_server;

-- 4. 소유권: public 의 테이블·뷰·독립 시퀀스 → lol_repository ------------
-- 컬럼 소속 시퀀스(IDENTITY·SERIAL)는 테이블 소유자를 따라가므로 따로 바꾸지 않는다 (바꾸면 오류).
DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT c.relname, c.relkind
        FROM pg_class c
        WHERE c.relnamespace = 'public'::regnamespace
          AND c.relkind IN ('r', 'p', 'v', 'm', 'S')
          AND NOT EXISTS (                       -- 컬럼 소속 시퀀스 제외
              SELECT 1 FROM pg_depend d
              WHERE d.classid = 'pg_class'::regclass
                AND d.objid = c.oid
                AND d.deptype IN ('a', 'i')
                AND d.refclassid = 'pg_class'::regclass)
          AND pg_get_userbyid(c.relowner) <> 'lol_repository'
    LOOP
        EXECUTE format('ALTER %s public.%I OWNER TO lol_repository',
                       CASE r.relkind WHEN 'S' THEN 'SEQUENCE'
                                      WHEN 'v' THEN 'VIEW'
                                      WHEN 'm' THEN 'MATERIALIZED VIEW'
                                      ELSE 'TABLE' END,
                       r.relname);
    END LOOP;
END
$$;

-- 5. lol_server 권한 -----------------------------------------------------
-- 재실행 시 잔여 권한을 지우고 다시 준다.
REVOKE ALL ON ALL TABLES    IN SCHEMA public FROM lol_server;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM lol_server;

-- Riot 데이터·배치 메타·뷰: 읽기만 (이행 기간 한정, P7 에서 회수)
GRANT SELECT ON ALL TABLES IN SCHEMA public TO lol_server;
REVOKE SELECT ON public.flyway_schema_history FROM lol_server;

-- 앱 테이블: 읽기·쓰기 + 소속 시퀀스
DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT c.relname
        FROM pg_class c
        WHERE c.relnamespace = 'public'::regnamespace
          AND c.relkind IN ('r', 'p')
          AND (c.relname IN ('member', 'member_withdrawal', 'social_account')
               OR c.relname LIKE 'community\_%'
               OR c.relname LIKE 'duo\_%')
    LOOP
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON public.%I TO lol_server', r.relname);
    END LOOP;

    FOR r IN
        SELECT s.relname
        FROM pg_class s
        JOIN pg_depend d ON d.classid = 'pg_class'::regclass AND d.objid = s.oid
                        AND d.deptype IN ('a', 'i') AND d.refclassid = 'pg_class'::regclass
        JOIN pg_class t ON t.oid = d.refobjid
        WHERE s.relkind = 'S'
          AND s.relnamespace = 'public'::regnamespace
          AND (t.relname IN ('member', 'member_withdrawal', 'social_account')
               OR t.relname LIKE 'community\_%'
               OR t.relname LIKE 'duo\_%')
    LOOP
        EXECUTE format('GRANT USAGE, SELECT ON SEQUENCE public.%I TO lol_server', r.relname);
    END LOOP;
END
$$;

-- 6. 이후 lol_repository(Flyway)가 public 에 만드는 객체 기본 권한 --------
-- 새 테이블은 lol_server 에 SELECT 만 열린다. 새 앱 테이블이 생기면 이 스크립트를 다시 돌려 RW 를 준다.
ALTER DEFAULT PRIVILEGES FOR ROLE lol_repository IN SCHEMA public
    GRANT SELECT ON TABLES TO lol_server;

COMMIT;

-- 결과 확인
\echo '--- 계정 · search_path'
SELECT r.rolname, s.setconfig
FROM pg_roles r
LEFT JOIN pg_db_role_setting s ON s.setrole = r.oid AND s.setdatabase = (SELECT oid FROM pg_database WHERE datname = current_database())
WHERE r.rolname IN ('lol_repository', 'lol_server')
ORDER BY 1;

\echo '--- lol_server 권한 요약 (테이블 수)'
SELECT CASE WHEN has_table_privilege('lol_server', c.oid, 'INSERT') THEN 'RW' ELSE 'SELECT' END AS lol_server,
       count(*)
FROM pg_class c
WHERE c.relnamespace = 'public'::regnamespace AND c.relkind IN ('r', 'p')
  AND has_table_privilege('lol_server', c.oid, 'SELECT')
GROUP BY 1 ORDER BY 1;
