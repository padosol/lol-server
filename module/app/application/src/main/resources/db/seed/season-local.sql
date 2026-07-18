-- local 개발용 season 기본 데이터 시드
-- postgresql-local.yml 의 spring.sql.init 로 매 부팅마다 실행되므로 ON CONFLICT 로 멱등 보장.
-- 스키마(테이블)는 Flyway(V1__init.sql)가 생성하며, 이 스크립트는 데이터만 채운다.

-- 시즌 16
INSERT INTO season (season_value, season_name, start_date, end_date, created_at)
VALUES (16, '2025 Season 2', '2025-05-01', NULL, now())
ON CONFLICT (season_value) DO NOTHING;

-- 패치 버전 16.10 ~ 16.14 (시즌 16 소속)
INSERT INTO patch_version (version_value, season_id, created_at)
SELECT v.version_value, s.season_id, now()
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
