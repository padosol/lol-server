-- Flyway 콜백: migrate 직전에 매번 실행된다 (버전 아님, 이력에 남지 않음). prod 만 locations 에 넣는다.
--
-- app 테이블이 아직 public 에 있으면(스키마 이동 전 DB) 기동을 거부한다.
-- 그대로 진행하면 app 스키마에 빈 테이블이 새로 생기고, search_path(app, riot, public) 상
-- 앞에 오는 그 빈 테이블이 public 의 실제 데이터를 가린다 — 회원·게시글이 사라진 것처럼 보이고
-- 새 쓰기는 엉뚱한 테이블로 간다.
--
-- local 에는 넣지 않는다: lol-repository 가 공유 체인(lol-db-schema)으로 public 에 app 테이블까지 만들기
-- 때문에(MP-143 전까지) 로컬에서는 항상 걸린다. 로컬의 public app 테이블은 쓰이지 않는 사본으로 남는다.
DO $$
BEGIN
    IF to_regclass('public.member') IS NOT NULL THEN
        RAISE EXCEPTION 'app 테이블이 아직 public 스키마에 있습니다. docs/db-split/02-move-schema.sql 로 app 으로 옮긴 뒤 기동하세요 (런북 docs/db-split/README.md).';
    END IF;
END
$$;
