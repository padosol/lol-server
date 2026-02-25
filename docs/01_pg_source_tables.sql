-- =====================================================
-- ClickHouse: PostgreSQL 소스 테이블 (PostgreSQL 테이블 엔진)
-- =====================================================
-- PostgreSQL 데이터를 ClickHouse에서 직접 읽기 위한 외부 테이블 정의
-- 연결 정보는 환경에 맞게 수정 필요
-- =====================================================

-- -----------------------------------------------------
-- 1-1. pg_match — 매치 기본 정보
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS pg_match
(
    id                  Int64,
    match_id            String,
--     game_version        Nullable(String),
    queue_id            Int32,
    platform_id         Nullable(String),
--     game_duration       Int64,
--     game_end_datetime   Nullable(DateTime),
    average_tier        Nullable(Int32),
    season              Int32,
    patch_version       Nullable(String)
)
ENGINE = PostgreSQL('125.138.61.176:41962', 'lol', 'match', 'METAPICK_DB', 'METAPICK_DB');

-- -----------------------------------------------------
-- 1-2. pg_match_summoner — 매치 참가자
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS pg_match_summoner
(
    id                  Int64,
    match_id            Nullable(String),
    puuid               Nullable(String),
    participant_id      Int32,
    team_id             Int32,
    champion_id         Int32,
    champion_name       Nullable(String),
    team_position       Nullable(String),
    lane                Nullable(String),
    win                 UInt8,
    tier                Nullable(String),
    tier_rank           Nullable(String),
    absolute_points     Nullable(Int32),
    item0               Int32,
    item1               Int32,
    item2               Int32,
    item3               Int32,
    item4               Int32,
    item5               Int32,
    item6               Int32,
    primary_rune_id     Int32,
    primary_rune_ids    Nullable(String),
    secondary_rune_id   Int32,
    secondary_rune_ids  Nullable(String),
--     offense             Int32,
--     flex                Int32,
--     defense             Int32,
    summoner1id         Int32,
    summoner2id         Int32
--     kills               Int32,
--     deaths              Int32,
--     assists             Int32,
--     gold_earned         Int32
--     total_damage_dealt_to_champions Int32,
--     total_minions_killed Int32,
--     neutral_minions_killed Int32,
--     vision_score        Int32
)
ENGINE = PostgreSQL('125.138.61.176:41962', 'lol', 'match_summoner', 'METAPICK_DB', 'METAPICK_DB');

-- -----------------------------------------------------
-- 1-3. pg_skill_events — 스킬 레벨업 이벤트 (PG VIEW 참조)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS pg_skill_events
(
    match_id         String,
    participant_id   Int32,
    skill_slot       Int32,
    timestamp        Int64
)
ENGINE = PostgreSQL('125.138.61.176:41962', 'lol', 'v_skill_events_flat', 'METAPICK_DB', 'METAPICK_DB');

-- -----------------------------------------------------
-- 1-4. pg_item_events — 아이템 구매 이벤트 (PG VIEW 참조)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS pg_item_events
(
    match_id         String,
    participant_id   Int32,
    item_id          Int32,
    timestamp        Int64
)
ENGINE = PostgreSQL('125.138.61.176:41962', 'lol', 'v_item_events_flat', 'METAPICK_DB', 'METAPICK_DB');
