-- MP-127 / MP-136 lol-server 계정 · app 스키마 상태 검증 (읽기 전용)
--
-- 실행: 슈퍼유저로.  psql -v ON_ERROR_STOP=1 -f 03-verify.sql
-- app 스키마가 있으면 이동 후(P2), 없으면 이동 전(P1)으로 보고 기대 상태를 검사한다.
-- 기대 상태: lol_server 는 app 스키마와 그 안의 객체만 소유하고, app 밖의 어떤 객체에도 권한이 없다.
-- 위반이 있으면 목록을 출력하고 마지막에 EXCEPTION 으로 실패한다.

\set ON_ERROR_STOP on

CREATE TEMP TABLE _violation (what text);

DO $$
DECLARE
    moved boolean := to_regnamespace('app') IS NOT NULL;
    r record;
BEGIN
    RAISE NOTICE 'phase = %', CASE WHEN moved THEN 'P2 (스키마 이동 후)' ELSE 'P1 (이동 전)' END;

    -- 1. 계정 · search_path
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'lol_server' AND rolcanlogin AND NOT rolsuper) THEN
        INSERT INTO _violation VALUES ('lol_server 계정 없음 또는 슈퍼유저');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_db_role_setting s JOIN pg_database d ON d.oid = s.setdatabase
                   WHERE d.datname = current_database() AND s.setrole = 'lol_server'::regrole
                     AND 'search_path=app' = ANY (s.setconfig)) THEN
        INSERT INTO _violation VALUES ('lol_server search_path ≠ app');
    END IF;

    -- 2. 테이블 · 뷰 · 시퀀스 (시스템 스키마 제외 전부)
    FOR r IN
        SELECT c.oid, n.nspname, c.relname, c.relkind,
               pg_get_userbyid(c.relowner) AS owner,
               (c.relname IN ('member', 'member_withdrawal', 'social_account')
                OR c.relname LIKE 'community\_%'
                OR c.relname LIKE 'duo\_%') AS is_app
        FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relkind IN ('r', 'p', 'v', 'm', 'S')
          AND n.nspname NOT IN ('pg_catalog', 'information_schema')
          AND n.nspname NOT LIKE 'pg\_toast%'
          AND n.nspname NOT LIKE 'pg\_temp%'
    LOOP
        IF r.nspname = 'app' THEN
            -- app 안: app 분류 테이블(+ 소속 시퀀스)과 Flyway 이력만, 전부 lol_server 소유
            IF r.relkind IN ('r', 'p') AND NOT r.is_app AND r.relname <> 'flyway_schema_history' THEN
                INSERT INTO _violation VALUES (format('app.%I: app 분류가 아닌 테이블', r.relname));
            END IF;
            IF r.owner <> 'lol_server' THEN
                INSERT INTO _violation VALUES (format('app.%I: 소유자가 lol_server 아님 (%s)', r.relname, r.owner));
            END IF;
        ELSE
            -- app 밖: P2 에서 app 테이블이 남아 있으면 안 되고, lol_server 는 어떤 권한도 없어야 한다
            IF moved AND r.is_app AND r.relkind IN ('r', 'p') THEN
                INSERT INTO _violation VALUES (format('%I.%I: app 테이블이 app 밖에 남아 있음', r.nspname, r.relname));
            END IF;
            IF r.owner = 'lol_server' THEN
                INSERT INTO _violation VALUES (format('%I.%I: app 밖 객체를 lol_server 가 소유', r.nspname, r.relname));
            ELSIF r.relkind = 'S' THEN
                IF has_sequence_privilege('lol_server', r.oid, 'USAGE')
                   OR has_sequence_privilege('lol_server', r.oid, 'SELECT')
                   OR has_sequence_privilege('lol_server', r.oid, 'UPDATE') THEN
                    INSERT INTO _violation VALUES (format('lol_server: app 밖 시퀀스 %I.%I 에 권한 있음', r.nspname, r.relname));
                END IF;
            -- has_table_privilege 에 여러 권한을 쉼표로 주면 "하나라도" 있으면 true
            ELSIF has_table_privilege('lol_server', r.oid, 'SELECT, INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER') THEN
                INSERT INTO _violation VALUES (format('lol_server: app 밖 %I.%I 에 권한 있음', r.nspname, r.relname));
            END IF;
        END IF;
    END LOOP;

    -- 3. 스키마 단위: app 은 lol_server 소유, 그 밖에는 CREATE 불가
    IF moved AND (SELECT pg_get_userbyid(nspowner) FROM pg_namespace WHERE nspname = 'app') <> 'lol_server' THEN
        INSERT INTO _violation VALUES ('app 스키마 소유자가 lol_server 아님');
    END IF;
    FOR r IN
        SELECT n.nspname FROM pg_namespace n
        WHERE n.nspname NOT IN ('app', 'pg_catalog', 'information_schema')
          AND n.nspname NOT LIKE 'pg\_toast%'
          AND n.nspname NOT LIKE 'pg\_temp%'
          AND has_schema_privilege('lol_server', n.oid, 'CREATE')
    LOOP
        INSERT INTO _violation VALUES (format('lol_server: %I 스키마에 CREATE 가능', r.nspname));
    END LOOP;
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
