-- MP-136 (P2) app 테이블 스키마 이동: public → app
--
-- 전제: 01-roles.sql 적용 완료, lol-server 가 lol_server 계정으로 동작 중 (MP-127), 런북 4.1.
-- lol-server 몫만 옮긴다. Riot 테이블 · 독립 시퀀스 · 뷰는 public 에 그대로 두고 lol-repository 가 따로 정한다.
-- 실행: 슈퍼유저로.  psql -v ON_ERROR_STOP=1 -f 02-move-schema.sql
--       잠금을 못 잡아 실패하면 아무것도 바뀌지 않은 채 롤백되므로 잠시 뒤 다시 실행한다.
-- 롤백: 02-rollback.sql
--
-- ALTER TABLE ... SET SCHEMA 는 카탈로그만 바꾼다 (데이터·파일·통계 불변). 인덱스·제약·컬럼 소속 시퀀스·GRANT 는
-- 테이블을 따라간다. app 과 Riot 테이블 사이에는 FK 가 없다 (있더라도 OID 로 묶여 있어 한쪽만 옮겨도 깨지지 않는다).
--
-- 잠금: app 테이블 전부를 LOCK TABLE 한 문장으로 먼저 잡는다.
--   - statement_timeout 이 그 한 문장에 걸리므로 잠금 대기의 "총합"이 묶인다
--     (lock_timeout 은 잠금 하나당 대기만 묶어서, 하나씩 잡으면 테이블 수 × 대기 시간까지 늘어날 수 있다).
--   - 대기 중에는 뒤따르는 조회도 그 테이블에서 줄을 서므로, 이 상한이 곧 lol-server 가 멈출 수 있는 최대 시간이다.
--     Riot 테이블은 잠그지 않으므로 lol-repository 는 영향받지 않는다.
--   - 다 잡은 뒤의 이동은 카탈로그 변경뿐이라 수십 ms.
--
-- 분류 (01·03 과 동일): app = member, member_withdrawal, social_account, community_*, duo_*  → app / 소유자 lol_server

\set ON_ERROR_STOP on
\timing on

BEGIN;

-- 1) 잠금 먼저: 총 대기 5초 상한 (잠금 하나당 2초). 대상이 없으면 null 이라 \gexec 가 건너뛰고 아래에서 실패한다.
SET LOCAL statement_timeout = '5s';
SET LOCAL lock_timeout = '2s';
SELECT format('LOCK TABLE %s IN ACCESS EXCLUSIVE MODE',
              string_agg(format('public.%I', c.relname), ', ' ORDER BY c.relname))
FROM pg_class c
WHERE c.relnamespace = 'public'::regnamespace
  AND c.relkind IN ('r', 'p')
  AND (c.relname IN ('member', 'member_withdrawal', 'social_account')
       OR c.relname LIKE 'community\_%'
       OR c.relname LIKE 'duo\_%')
\gexec

-- 2) 이동: 잠금을 이미 쥐고 있으므로 짧다
SET LOCAL statement_timeout = '10s';

CREATE SCHEMA IF NOT EXISTS app AUTHORIZATION lol_server;

DO $$
DECLARE
    r record;
    moved int := 0;
BEGIN
    FOR r IN
        SELECT c.relname
        FROM pg_class c
        WHERE c.relnamespace = 'public'::regnamespace
          AND c.relkind IN ('r', 'p')
          AND (c.relname IN ('member', 'member_withdrawal', 'social_account')
               OR c.relname LIKE 'community\_%'
               OR c.relname LIKE 'duo\_%')
        ORDER BY c.relname
    LOOP
        EXECUTE format('ALTER TABLE public.%I SET SCHEMA app', r.relname);
        EXECUTE format('ALTER TABLE app.%I OWNER TO lol_server', r.relname);  -- 컬럼 소속 시퀀스도 함께
        moved := moved + 1;
    END LOOP;

    IF moved = 0 THEN
        RAISE EXCEPTION 'public 에 app 테이블이 없음 — 이미 옮겼거나 대상 DB 가 다름';
    END IF;
    RAISE NOTICE 'moved tables: app=%', moved;
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
WHERE n.nspname IN ('public', 'app') AND c.relkind IN ('r', 'p', 'S', 'v', 'm')
GROUP BY 1, 2 ORDER BY 1, 2;
