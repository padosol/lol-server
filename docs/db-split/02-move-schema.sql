-- MP-128 / MP-136 (P2) 테이블 스키마 이동: public → app · riot
--
-- 전제: 01-roles.sql 적용 완료, 두 서비스가 서비스 계정으로 동작 중 (MP-127),
--       lol-repository 에 spring.flyway.default-schema=public 배포 완료, 백필·랭킹 계산 미실행 (런북 4.1).
-- 실행: 슈퍼유저로.  psql -v ON_ERROR_STOP=1 -f 02-move-schema.sql
--       잠금을 못 잡아 실패하면 아무것도 바뀌지 않은 채 롤백되므로 잠시 뒤 다시 실행한다.
-- 롤백: 02-rollback.sql
--
-- ALTER ... SET SCHEMA 는 카탈로그만 바꾼다 (데이터·파일·통계 불변). 인덱스·제약·컬럼 소속 시퀀스·GRANT 는
-- 테이블을 따라간다. 독립 시퀀스(batch_*_seq)와 뷰는 따라가지 않으므로 따로 옮긴다.
--
-- 잠금: 이동 대상 테이블·뷰 전부를 LOCK TABLE 한 문장으로 먼저 잡는다.
--   - statement_timeout 이 그 한 문장에 걸리므로 잠금 대기의 "총합"이 묶인다
--     (lock_timeout 은 잠금 하나당 대기만 묶어서, 하나씩 잡으면 64 × 대기 시간까지 늘어날 수 있다).
--   - 대기 중에는 뒤따르는 조회도 그 테이블에서 줄을 서므로, 이 상한이 곧 서비스가 멈출 수 있는 최대 시간이다.
--   - 다 잡은 뒤의 이동은 카탈로그 변경뿐이라 수십 ms.
--
-- 분류 (01·03 과 동일):
--   app  = member, member_withdrawal, social_account, community_*, duo_*   → 소유자 lol_server
--   riot = 그 외 테이블 · 독립 시퀀스 · 뷰                                 → 소유자 lol_repository
--   public 에 남는 것: flyway_schema_history (이력 보존), 확장 btree_gist

\set ON_ERROR_STOP on
\timing on

BEGIN;

-- 1) 잠금 먼저: 총 대기 5초 상한 (잠금 하나당 2초)
SET LOCAL statement_timeout = '5s';
SET LOCAL lock_timeout = '2s';
SELECT format('LOCK TABLE %s IN ACCESS EXCLUSIVE MODE',
              string_agg(format('public.%I', c.relname), ', ' ORDER BY c.relname))
FROM pg_class c
WHERE c.relnamespace = 'public'::regnamespace
  AND c.relkind IN ('r', 'p', 'v', 'm')
  AND c.relname <> 'flyway_schema_history'
\gexec

-- 2) 이동: 잠금을 이미 쥐고 있으므로 짧다
SET LOCAL statement_timeout = '10s';

CREATE SCHEMA IF NOT EXISTS riot AUTHORIZATION lol_repository;
CREATE SCHEMA IF NOT EXISTS app  AUTHORIZATION lol_server;

-- 이행 기간(P7 전까지) lol_server 의 riot 읽기. 테이블 SELECT 권한은 01 에서 준 것이 테이블을 따라온다.
GRANT USAGE ON SCHEMA riot TO lol_server;
ALTER DEFAULT PRIVILEGES FOR ROLE lol_repository IN SCHEMA riot
    GRANT SELECT ON TABLES TO lol_server;

DO $$
DECLARE
    r record;
    moved_app  int := 0;
    moved_riot int := 0;
BEGIN
    -- 1. 테이블
    FOR r IN
        SELECT c.relname,
               (c.relname IN ('member', 'member_withdrawal', 'social_account')
                OR c.relname LIKE 'community\_%'
                OR c.relname LIKE 'duo\_%') AS is_app
        FROM pg_class c
        WHERE c.relnamespace = 'public'::regnamespace
          AND c.relkind IN ('r', 'p')
          AND c.relname <> 'flyway_schema_history'
        ORDER BY c.relname
    LOOP
        IF r.is_app THEN
            EXECUTE format('ALTER TABLE public.%I SET SCHEMA app', r.relname);
            EXECUTE format('ALTER TABLE app.%I OWNER TO lol_server', r.relname);  -- 컬럼 소속 시퀀스도 함께
            moved_app := moved_app + 1;
        ELSE
            EXECUTE format('ALTER TABLE public.%I SET SCHEMA riot', r.relname);
            moved_riot := moved_riot + 1;
        END IF;
    END LOOP;

    -- 2. 독립 시퀀스 (batch_job_seq, batch_job_execution_seq, batch_step_execution_seq)
    FOR r IN
        SELECT c.relname
        FROM pg_class c
        WHERE c.relnamespace = 'public'::regnamespace
          AND c.relkind = 'S'
          AND NOT EXISTS (
              SELECT 1 FROM pg_depend d
              WHERE d.classid = 'pg_class'::regclass AND d.objid = c.oid
                AND d.deptype IN ('a', 'i') AND d.refclassid = 'pg_class'::regclass)
    LOOP
        EXECUTE format('ALTER SEQUENCE public.%I SET SCHEMA riot', r.relname);
    END LOOP;

    -- 3. 뷰 (분석용, 앱 미사용)
    FOR r IN
        SELECT c.relname, c.relkind
        FROM pg_class c
        WHERE c.relnamespace = 'public'::regnamespace AND c.relkind IN ('v', 'm')
    LOOP
        EXECUTE format('ALTER %s public.%I SET SCHEMA riot',
                       CASE r.relkind WHEN 'm' THEN 'MATERIALIZED VIEW' ELSE 'VIEW' END, r.relname);
    END LOOP;

    RAISE NOTICE 'moved tables: app=%, riot=%', moved_app, moved_riot;

    -- 4. 검증: public 에는 flyway_schema_history 만 남아야 한다
    IF EXISTS (
        SELECT 1 FROM pg_class c
        WHERE c.relnamespace = 'public'::regnamespace
          AND c.relkind IN ('r', 'p', 'v', 'm', 'S')
          AND c.relname <> 'flyway_schema_history')
    THEN
        RAISE EXCEPTION 'public 에 옮기지 않은 객체가 남아 있음 — 롤백';
    END IF;
END
$$;

COMMIT;

\timing off
\echo '--- 스키마별 객체 수'
SELECT n.nspname AS schema, pg_get_userbyid(c.relowner) AS owner,
       count(*) FILTER (WHERE c.relkind IN ('r', 'p')) AS tables,
       count(*) FILTER (WHERE c.relkind = 'S')        AS sequences,
       count(*) FILTER (WHERE c.relkind IN ('v', 'm')) AS views
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname IN ('public', 'app', 'riot') AND c.relkind IN ('r', 'p', 'S', 'v', 'm')
GROUP BY 1, 2 ORDER BY 1, 2;
