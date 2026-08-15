-- local 개발용 season 기본 데이터 시드
-- postgresql-local.yml 의 spring.sql.init 로 매 부팅마다 실행되므로 ON CONFLICT 로 멱등 보장.
-- 스키마(테이블)는 Flyway(V1__init.sql)가 생성하며, 이 스크립트는 데이터만 채운다.

-- 시즌 16
INSERT INTO season (season_value, season_name, start_date, end_date, created_at)
VALUES (16, '2025 Season 2', '2025-05-01', NULL, now())
ON CONFLICT (season_value) DO NOTHING;

-- 패치 버전 16.10 ~ 16.14 (시즌 16 소속)
-- patch_version_data 는 Data Dragon 정적 데이터 버전이다. 패치 버전에 마지막 자리를
-- 붙인 형태(16.14 -> 16.14.1)라 로컬에서는 그대로 파생시켜 넣는다.
INSERT INTO patch_version (version_value, patch_version_data, season_id, created_at)
SELECT v.version_value, v.version_value || '.1', s.season_id, now()
FROM season s
CROSS JOIN (VALUES
    ('16.10'),
    ('16.11'),
    ('16.12'),
    ('16.13'),
    ('16.14')
) AS v(version_value)
WHERE s.season_value = 16
ON CONFLICT (version_value) DO NOTHING;

-- 이미 patch_version 행이 있는 로컬 DB 백필.
-- 위 INSERT 는 ON CONFLICT DO NOTHING 이라 기존 행의 새 컬럼을 채우지 못한다.
-- 운영 DB 는 값을 지어내지 않기 위해 마이그레이션(V35)에서 백필을 하지 않으므로,
-- 이 스크립트를 읽는 local 프로파일에서만 채워진다.
-- 2자리(x.y) 행만 대상으로 삼아, 이미 3자리가 들어 있는 행은 건드리지 않는다.
UPDATE patch_version
   SET patch_version_data = version_value || '.1'
 WHERE patch_version_data IS NULL
   AND version_value ~ '^[0-9]+\.[0-9]+$';
