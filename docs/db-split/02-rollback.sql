-- MP-136 롤백: app → public
--
-- 02-move-schema.sql 의 역방향. 데이터는 바뀌지 않았으므로 즉시 끝난다.
-- 실행: 슈퍼유저로.  psql -v ON_ERROR_STOP=1 -f 02-rollback.sql
--       app 테이블 소유자는 실행한 계정(이동 전 소유자인 POSTGRES_USER)으로 돌아간다. 다르면 -v owner=<계정>.
-- 주의: app 에 이동 후 새로 만든 테이블(V2 의 community_image 등)도 함께 public 으로 돌아간다. 이름이 public 의 것과 겹치면 실패한다.
-- 주의: lol-server 새 이미지를 롤아웃했다면 먼저 이전 이미지로 되돌린다 — 새 이미지는 app 테이블이 public 에 있으면
--       기동을 거부한다 (db/migration-guard/beforeMigrate.sql). lol_server 계정으로 전환한 뒤라면 자격증명도 먼저 되돌린다.
-- 잠금은 02-move-schema.sql 과 같은 방식: 전부 한 문장으로 먼저 잡고 총 대기 5초 상한. 실패하면 다시 실행.

\set ON_ERROR_STOP on
\timing on

\if :{?owner}
\else
  SELECT current_user AS owner \gset
\endif

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
SET LOCAL db_split.owner = :'owner';

-- lol-server Flyway(app 체인) 이력. 롤백 뒤에는 쓸모가 없고, public 에 같은 이름이 있으면 옮길 수도 없다.
-- 다시 이동하면 lol-server 가 기동하며 baseline 부터 새로 쓴다 (V2 이후는 IF NOT EXISTS 라 재실행 무해).
DROP TABLE IF EXISTS app.flyway_schema_history;

DO $$
DECLARE
    r record;
    owner_name text := current_setting('db_split.owner');
BEGIN
    FOR r IN
        SELECT c.relname FROM pg_class c
        WHERE c.relnamespace = 'app'::regnamespace AND c.relkind IN ('r', 'p')
    LOOP
        EXECUTE format('ALTER TABLE app.%I OWNER TO %I', r.relname, owner_name);  -- 컬럼 소속 시퀀스도 함께
        EXECUTE format('ALTER TABLE app.%I SET SCHEMA public', r.relname);
    END LOOP;
END
$$;

-- 비어 있지 않으면 실패한다 (CASCADE 쓰지 않음)
DROP SCHEMA app;

COMMIT;

\timing off
\echo '--- public 의 app 테이블 수 (소유자별)'
SELECT pg_get_userbyid(c.relowner) AS owner, count(*) AS tables
FROM pg_class c
WHERE c.relnamespace = 'public'::regnamespace AND c.relkind IN ('r', 'p')
  AND (c.relname IN ('member', 'member_withdrawal', 'social_account')
       OR c.relname LIKE 'community\_%'
       OR c.relname LIKE 'duo\_%')
GROUP BY 1 ORDER BY 1;
