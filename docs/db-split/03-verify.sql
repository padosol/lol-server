-- MP-136 app 스키마 상태 검증 (읽기 전용)
--
-- 실행: 슈퍼유저로.  psql -v ON_ERROR_STOP=1 -f 03-verify.sql
-- app 스키마가 있으면 이동 후(P2), 없으면 이동 전(P1)으로 보고 기대 상태를 검사한다.
-- 기대 상태: app 테이블(분류는 02 와 동일)이 P1 이면 전부 public 에, P2 이면 전부 app 에 있고,
--           app 스키마에는 app 테이블과 Flyway 이력만 있다.
-- 위반이 있으면 목록을 출력하고 마지막에 EXCEPTION 으로 실패한다.

\set ON_ERROR_STOP on

CREATE TEMP TABLE _violation (what text);

DO $$
DECLARE
    moved boolean := to_regnamespace('app') IS NOT NULL;
    home text := CASE WHEN to_regnamespace('app') IS NOT NULL THEN 'app' ELSE 'public' END;
    r record;
    app_tables int := 0;
BEGIN
    RAISE NOTICE 'phase = %', CASE WHEN moved THEN 'P2 (스키마 이동 후)' ELSE 'P1 (이동 전)' END;

    FOR r IN
        SELECT n.nspname, c.relname,
               (c.relname IN ('member', 'member_withdrawal', 'social_account')
                OR c.relname LIKE 'community\_%'
                OR c.relname LIKE 'duo\_%') AS is_app
        FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relkind IN ('r', 'p')
          AND n.nspname NOT IN ('pg_catalog', 'information_schema')
          AND n.nspname NOT LIKE 'pg\_toast%'
          AND n.nspname NOT LIKE 'pg\_temp%'
    LOOP
        IF r.is_app AND r.nspname = home THEN
            app_tables := app_tables + 1;
        ELSIF r.is_app THEN
            INSERT INTO _violation VALUES (format('%I.%I: app 테이블이 %s 밖에 있음', r.nspname, r.relname, home));
        ELSIF r.nspname = 'app' AND r.relname <> 'flyway_schema_history' THEN
            INSERT INTO _violation VALUES (format('app.%I: app 분류가 아닌 테이블', r.relname));
        END IF;
    END LOOP;

    RAISE NOTICE 'app 테이블: % 스키마에 %개', home, app_tables;
    IF app_tables = 0 THEN
        INSERT INTO _violation VALUES (format('%s 에 app 테이블이 없음 — 대상 DB 확인', home));
    END IF;
END
$$;

\echo '--- 위반 목록 (비어 있어야 함)'
SELECT what FROM _violation ORDER BY 1;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM _violation) THEN
        RAISE EXCEPTION '검증 실패: % 건', (SELECT count(*) FROM _violation);
    END IF;
    RAISE NOTICE '검증 통과';
END
$$;
