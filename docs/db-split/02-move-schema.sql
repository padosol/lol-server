-- MP-136 app 테이블 스키마 이동: public → app
--
-- 전제: 런북 4.1.
-- lol-server 몫(app 테이블)만 옮긴다. 그 밖의 객체는 그대로 두고 잠그지도 않는다.
-- 실행: 슈퍼유저(lol-server 가 접속하는 계정)로.  psql -v ON_ERROR_STOP=1 -f 02-move-schema.sql
-- 소유자는 바꾸지 않는다 — app 스키마는 실행 계정 소유, 테이블은 지금 소유자 그대로.
--       잠금을 못 잡아 실패하면 아무것도 바뀌지 않은 채 롤백되므로 잠시 뒤 다시 실행한다.
-- 롤백: 02-rollback.sql
--
-- ALTER TABLE ... SET SCHEMA 는 카탈로그만 바꾼다 (데이터·파일·통계 불변). 인덱스·제약·컬럼 소속 시퀀스·GRANT 는
-- 테이블을 따라간다. app 테이블은 app 밖 테이블과 FK 로 묶여 있지 않다 (묶여 있더라도 OID 기준이라 깨지지 않는다).
--
-- 잠금: app 테이블 전부를 LOCK TABLE 한 문장으로 먼저 잡는다.
--   - statement_timeout 이 그 한 문장에 걸리므로 잠금 대기의 "총합"이 묶인다
--     (lock_timeout 은 잠금 하나당 대기만 묶어서, 하나씩 잡으면 테이블 수 × 대기 시간까지 늘어날 수 있다).
--   - 대기 중에는 뒤따르는 조회도 그 테이블에서 줄을 서므로, 이 상한이 곧 lol-server 가 멈출 수 있는 최대 시간이다.
--   - 다 잡은 뒤의 이동은 카탈로그 변경뿐이라 수십 ms.
--
-- 분류 (03 과 동일): app = member, member_withdrawal, social_account, community_*, duo_*  → app

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

CREATE SCHEMA IF NOT EXISTS app;

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
        EXECUTE format('ALTER TABLE public.%I SET SCHEMA app', r.relname);  -- 컬럼 소속 시퀀스도 함께
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
\echo '--- app 스키마 객체 수'
SELECT pg_get_userbyid(c.relowner) AS owner,
       count(*) FILTER (WHERE c.relkind IN ('r', 'p')) AS tables,
       count(*) FILTER (WHERE c.relkind = 'S')        AS sequences
FROM pg_class c
WHERE c.relnamespace = 'app'::regnamespace AND c.relkind IN ('r', 'p', 'S')
GROUP BY 1 ORDER BY 1;
