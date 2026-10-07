-- MP-128 / MP-136 (P2) 롤백: app · riot → public
--
-- 02-move-schema.sql 의 역방향. 데이터는 바뀌지 않았으므로 즉시 끝난다.
-- 실행: 슈퍼유저로.  psql -v ON_ERROR_STOP=1 -f 02-rollback.sql
-- 결과 상태 = 01-roles.sql 직후 (public 전 객체 소유 lol_repository, lol_server 는 앱 RW · 그 외 SELECT).
-- 주의: app/riot 에 이동 후 새로 만든 객체가 있으면 함께 public 으로 돌아간다. 이름이 public 의 것과 겹치면 실패한다.

\set ON_ERROR_STOP on
\timing on

BEGIN;
SET LOCAL lock_timeout = '3s';

DO $$
DECLARE
    r record;
BEGIN
    -- 뷰 → 독립 시퀀스 → 테이블 순 (이동의 역순)
    FOR r IN
        SELECT c.relname, c.relkind
        FROM pg_class c
        WHERE c.relnamespace = 'riot'::regnamespace AND c.relkind IN ('v', 'm')
    LOOP
        EXECUTE format('ALTER %s riot.%I SET SCHEMA public',
                       CASE r.relkind WHEN 'm' THEN 'MATERIALIZED VIEW' ELSE 'VIEW' END, r.relname);
    END LOOP;

    FOR r IN
        SELECT c.relname
        FROM pg_class c
        WHERE c.relnamespace = 'riot'::regnamespace
          AND c.relkind = 'S'
          AND NOT EXISTS (
              SELECT 1 FROM pg_depend d
              WHERE d.classid = 'pg_class'::regclass AND d.objid = c.oid
                AND d.deptype IN ('a', 'i') AND d.refclassid = 'pg_class'::regclass)
    LOOP
        EXECUTE format('ALTER SEQUENCE riot.%I SET SCHEMA public', r.relname);
    END LOOP;

    FOR r IN
        SELECT c.relname FROM pg_class c
        WHERE c.relnamespace = 'riot'::regnamespace AND c.relkind IN ('r', 'p')
    LOOP
        EXECUTE format('ALTER TABLE riot.%I SET SCHEMA public', r.relname);
    END LOOP;

    FOR r IN
        SELECT c.relname FROM pg_class c
        WHERE c.relnamespace = 'app'::regnamespace AND c.relkind IN ('r', 'p')
    LOOP
        EXECUTE format('ALTER TABLE app.%I OWNER TO lol_repository', r.relname);
        EXECUTE format('ALTER TABLE app.%I SET SCHEMA public', r.relname);
        -- 소유자가 lol_server 였던 동안 lol_server 앞 GRANT 는 소유자 권한에 흡수됐다가
        -- 소유자 변경으로 사라진다 → 01-roles.sql 5단계와 같은 권한을 다시 준다.
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

ALTER DEFAULT PRIVILEGES FOR ROLE lol_repository IN SCHEMA riot
    REVOKE SELECT ON TABLES FROM lol_server;

-- 비어 있지 않으면 실패한다 (CASCADE 쓰지 않음)
DROP SCHEMA riot;
DROP SCHEMA app;

COMMIT;

\timing off
\echo '--- public 객체 수 (소유자별)'
SELECT pg_get_userbyid(c.relowner) AS owner,
       count(*) FILTER (WHERE c.relkind IN ('r', 'p')) AS tables,
       count(*) FILTER (WHERE c.relkind = 'S')        AS sequences,
       count(*) FILTER (WHERE c.relkind IN ('v', 'm')) AS views
FROM pg_class c
WHERE c.relnamespace = 'public'::regnamespace AND c.relkind IN ('r', 'p', 'S', 'v', 'm')
GROUP BY 1 ORDER BY 1;
