-- =====================================================
-- 01: PostgreSQL Engine 외부 테이블 (수동 실행)
-- =====================================================
-- 이 파일은 PostgreSQL 접속 정보를 포함하므로 .gitignore 에 등록되어 있습니다.
-- docker exec -it clickhouse_analytics clickhouse-client 접속 후 수동 실행하세요.
-- =====================================================

CREATE TABLE IF NOT EXISTS pg_match
(
    match_id      String,
    queue_id      Int32,
    platform_id   Nullable(String),
    patch_version Nullable(String)
)
ENGINE = PostgreSQL('125.138.61.176:41962', 'lol', 'match', 'METAPICK_DB', 'METAPICK_DB');

CREATE TABLE IF NOT EXISTS pg_match_participant
(
    match_id       String,
    champion_id    Int32,
    team_position  Nullable(String),
    team_id        Int32,
    win            UInt8,
    tier           Nullable(String),
    participant_id Int32
)
ENGINE = PostgreSQL('125.138.61.176:41962', 'lol', 'match_participant', 'METAPICK_DB', 'METAPICK_DB');

CREATE TABLE IF NOT EXISTS pg_match_ban
(
    match_id    String,
    team_id     Int32,
    champion_id Int32,
    pick_turn   Int32
)
ENGINE = PostgreSQL('125.138.61.176:41962', 'lol', 'match_ban', 'METAPICK_DB', 'METAPICK_DB');
