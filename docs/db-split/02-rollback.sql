-- MP-136 (P2) 롤백: app → public
--
-- 02-move-schema.sql 의 역방향. 데이터는 바뀌지 않았으므로 즉시 끝난다.
-- 실행: 슈퍼유저로.  psql -v ON_ERROR_STOP=1 -f 02-rollback.sql
-- 결과 상태 = 01-roles.sql 직후 (app 테이블은 public, 소유자는 Riot 테이블을 만드는 계정, lol_server 는 앱 RW · 그 외 SELECT).
-- 주의: app 에 이동 후 새로 만든 테이블(V2 의 community_image 등)도 함께 public 으로 돌아간다. 이름이 public 의 것과 겹치면 실패한다.
-- 주의: lol-server 가 app Flyway 체인으로 기동한 뒤라면 lol-server 이미지를 먼저 이전 버전으로 되돌린다.
--       새 이미지는 app 테이블이 public 에 있으면 기동을 거부한다 (db/migration-guard/beforeMigrate.sql).
-- 잠금은 02-move-schema.sql 과 같은 방식: 전부 한 문장으로 먼저 잡고 총 대기 5초 상한. 실패하면 다시 실행.

\set ON_ERROR_STOP on
\timing on

-- 돌려줄 소유자 = Riot 테이블을 만드는 계정 (01-roles.sql 과 같은 기준)
SELECT coalesce((SELECT pg_get_userbyid(relowner) FROM pg_class
                 WHERE oid = to_regclass('public.flyway_schema_history')), current_user) AS riot_owner \gset

BEGIN;

SET LOCAL statement_timeout = '5s';
SET LOCAL lock_timeout = '2s';
SELECT format('LOCK TABLE %s IN ACCESS EXCLUSIVE MODE',
              string_agg(format('app.%I', c.relname), ', ' ORDER BY c.relname))
FROM pg_class c
WHERE c.relnamespace = 'app'::regnamespace
  AND c.relkind IN ('r', 'p')
\gexec

SET LOCAL statement_timeout = '10s';
SET LOCAL db_split.riot_owner = :'riot_owner';

-- lol-server Flyway(app 체인) 이력. public 의 것과 이름이 겹쳐 옮길 수 없고, 롤백 뒤에는 쓸모가 없다.
-- 다시 이동하면 lol-server 가 기동하며 baseline 부터 새로 쓴다 (V2 이후는 IF NOT EXISTS 라 재실행 무해).
DROP TABLE IF EXISTS app.flyway_schema_history;

DO $$
DECLARE
    r record;
    owner_name text := current_setting('db_split.riot_owner');
BEGIN
    FOR r IN
        SELECT c.relname FROM pg_class c
        WHERE c.relnamespace = 'app'::regnamespace AND c.relkind IN ('r', 'p')
    LOOP
        EXECUTE format('ALTER TABLE app.%I OWNER TO %I', r.relname, owner_name);
        EXECUTE format('ALTER TABLE app.%I SET SCHEMA public', r.relname);
        -- 소유자가 lol_server 였던 동안 lol_server 앞 GRANT 는 소유자 권한에 흡수됐다가
        -- 소유자 변경으로 사라진다 → 01-roles.sql 4단계와 같은 권한을 다시 준다.
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

-- 비어 있지 않으면 실패한다 (CASCADE 쓰지 않음)
DROP SCHEMA app;

COMMIT;

\timing off
\echo '--- public 테이블 수 (소유자별)'
SELECT pg_get_userbyid(c.relowner) AS owner, count(*) AS tables
FROM pg_class c
WHERE c.relnamespace = 'public'::regnamespace AND c.relkind IN ('r', 'p')
GROUP BY 1 ORDER BY 1;
