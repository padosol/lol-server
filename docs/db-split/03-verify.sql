-- MP-120 / MP-128 계정·권한·스키마 상태 검증 (읽기 전용)
--
-- 실행: 슈퍼유저로.  psql -v ON_ERROR_STOP=1 -f 03-verify.sql
-- riot 스키마가 있으면 이동 후(P2), 없으면 이동 전(P1)으로 보고 기대 상태를 검사한다.
-- 위반이 있으면 목록을 출력하고 마지막에 EXCEPTION 으로 실패한다.

\set ON_ERROR_STOP on

CREATE TEMP TABLE _violation (what text);

DO $$
DECLARE
    moved boolean := to_regnamespace('riot') IS NOT NULL;
    r record;
BEGIN
    RAISE NOTICE 'phase = %', CASE WHEN moved THEN 'P2 (스키마 이동 후)' ELSE 'P1 (이동 전)' END;

    -- 1. 계정 · search_path
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'lol_repository' AND rolcanlogin AND NOT rolsuper) THEN
        INSERT INTO _violation VALUES ('lol_repository 계정 없음 또는 슈퍼유저');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'lol_server' AND rolcanlogin AND NOT rolsuper) THEN
        INSERT INTO _violation VALUES ('lol_server 계정 없음 또는 슈퍼유저');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_db_role_setting s JOIN pg_database d ON d.oid = s.setdatabase
                   WHERE d.datname = current_database() AND s.setrole = 'lol_repository'::regrole
                     AND 'search_path=riot, public' = ANY (s.setconfig)) THEN
        INSERT INTO _violation VALUES ('lol_repository search_path ≠ riot, public');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_db_role_setting s JOIN pg_database d ON d.oid = s.setdatabase
                   WHERE d.datname = current_database() AND s.setrole = 'lol_server'::regrole
                     AND 'search_path=app, riot, public' = ANY (s.setconfig)) THEN
        INSERT INTO _violation VALUES ('lol_server search_path ≠ app, riot, public');
    END IF;

    -- 2. 테이블 단위 권한
    FOR r IN
        SELECT c.oid, n.nspname, c.relname,
               (c.relname IN ('member', 'member_withdrawal', 'social_account')
                OR c.relname LIKE 'community\_%'
                OR c.relname LIKE 'duo\_%') AS is_app
        FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname IN ('public', 'app', 'riot')
          AND c.relkind IN ('r', 'p')
          AND c.relname <> 'flyway_schema_history'
    LOOP
        -- 위치
        IF moved AND r.nspname <> (CASE WHEN r.is_app THEN 'app' ELSE 'riot' END) THEN
            INSERT INTO _violation VALUES (format('%I.%I: 잘못된 스키마', r.nspname, r.relname));
        END IF;

        -- lol_server: 앱 = RW, 그 외 = SELECT 만
        IF NOT has_table_privilege('lol_server', r.oid, 'SELECT') THEN
            INSERT INTO _violation VALUES (format('lol_server: %I.%I SELECT 없음', r.nspname, r.relname));
        END IF;
        -- has_table_privilege 에 여러 권한을 쉼표로 주면 "하나라도" 있으면 true → 따로 검사
        IF r.is_app AND NOT (has_table_privilege('lol_server', r.oid, 'INSERT')
                             AND has_table_privilege('lol_server', r.oid, 'UPDATE')
                             AND has_table_privilege('lol_server', r.oid, 'DELETE')) THEN
            INSERT INTO _violation VALUES (format('lol_server: %I.%I 쓰기 권한 부족', r.nspname, r.relname));
        END IF;
        IF NOT r.is_app AND has_table_privilege('lol_server', r.oid, 'INSERT') THEN
            INSERT INTO _violation VALUES (format('lol_server: Riot 테이블 %I.%I 에 INSERT 가능', r.nspname, r.relname));
        END IF;
        IF NOT r.is_app AND (has_table_privilege('lol_server', r.oid, 'UPDATE')
                             OR has_table_privilege('lol_server', r.oid, 'DELETE')
                             OR has_table_privilege('lol_server', r.oid, 'TRUNCATE')) THEN
            INSERT INTO _violation VALUES (format('lol_server: Riot 테이블 %I.%I 에 UPDATE/DELETE/TRUNCATE 가능', r.nspname, r.relname));
        END IF;

        -- lol_repository: Riot = 소유, 앱 = P1 소유(전환기) / P2 권한 없음
        IF NOT r.is_app AND pg_get_userbyid((SELECT relowner FROM pg_class WHERE oid = r.oid)) <> 'lol_repository' THEN
            INSERT INTO _violation VALUES (format('%I.%I 소유자가 lol_repository 아님', r.nspname, r.relname));
        END IF;
        IF r.is_app AND moved AND has_table_privilege('lol_repository', r.oid, 'SELECT') THEN
            INSERT INTO _violation VALUES (format('lol_repository: 앱 테이블 %I.%I 접근 가능', r.nspname, r.relname));
        END IF;
    END LOOP;

    -- 3. 앱 테이블 소속 시퀀스: lol_server USAGE
    FOR r IN
        SELECT s.oid, n.nspname, s.relname
        FROM pg_class s
        JOIN pg_namespace n ON n.oid = s.relnamespace
        JOIN pg_depend d ON d.classid = 'pg_class'::regclass AND d.objid = s.oid
                        AND d.deptype IN ('a', 'i') AND d.refclassid = 'pg_class'::regclass
        JOIN pg_class t ON t.oid = d.refobjid
        WHERE s.relkind = 'S'
          AND (t.relname IN ('member', 'member_withdrawal', 'social_account')
               OR t.relname LIKE 'community\_%'
               OR t.relname LIKE 'duo\_%')
    LOOP
        IF NOT has_sequence_privilege('lol_server', r.oid, 'USAGE') THEN
            INSERT INTO _violation VALUES (format('lol_server: 시퀀스 %I.%I USAGE 없음', r.nspname, r.relname));
        END IF;
    END LOOP;

    -- 4. 스키마 단위
    IF has_schema_privilege('lol_server', 'public', 'CREATE') THEN
        INSERT INTO _violation VALUES ('lol_server: public 에 CREATE 가능');
    END IF;
    IF moved THEN
        IF has_schema_privilege('lol_repository', 'app', 'USAGE') THEN
            INSERT INTO _violation VALUES ('lol_repository: app 스키마 USAGE 가능');
        END IF;
        IF has_schema_privilege('lol_server', 'riot', 'CREATE') THEN
            INSERT INTO _violation VALUES ('lol_server: riot 에 CREATE 가능');
        END IF;
        IF EXISTS (SELECT 1 FROM pg_class c
                   WHERE c.relnamespace = 'public'::regnamespace
                     AND c.relkind IN ('r', 'p', 'v', 'm')
                     AND c.relname <> 'flyway_schema_history') THEN
            INSERT INTO _violation VALUES ('public 에 flyway_schema_history 외 테이블·뷰가 남아 있음');
        END IF;
        IF EXISTS (SELECT 1 FROM pg_class c
                   WHERE c.relnamespace = 'public'::regnamespace AND c.relkind = 'S') THEN
            INSERT INTO _violation VALUES ('public 에 시퀀스가 남아 있음 (batch_*_seq 이동 누락?)');
        END IF;
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
