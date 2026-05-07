-- =====================================================
-- 01: PostgreSQL Engine 외부 테이블 (수동 실행)
-- =====================================================
-- DEPRECATED (MP-36): 챔피언 통계 OLAP 은 BigQuery 로 이전됐고, ClickHouse 어댑터/모듈은 폐기됐습니다.
-- 이 파일은 과거 ClickHouse 기반 운영 시점의 정의를 참고용으로만 남겨둡니다 — 현 운영에 사용되지 않습니다.
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
    match_id          String,
    champion_id       Int32,
    team_position     Nullable(String),
    team_id           Int32,
    win               UInt8,
    tier              Nullable(String),
    participant_id    Int32,
    primary_style_id  Nullable(Int32),
    primary_perk0     Nullable(Int32),
    primary_perk1     Nullable(Int32),
    primary_perk2     Nullable(Int32),
    primary_perk3     Nullable(Int32),
    sub_style_id      Nullable(Int32),
    sub_perk0         Nullable(Int32),
    sub_perk1         Nullable(Int32),
    stat_perk_defense Nullable(Int32),
    stat_perk_flex    Nullable(Int32),
    stat_perk_offense Nullable(Int32),
    summoner1id       Int32,
    summoner2id       Int32,
    item0             Int32,
    item1             Int32,
    item2             Int32,
    item3             Int32,
    item4             Int32,
    item5             Int32
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

CREATE TABLE IF NOT EXISTS pg_skill_events_flat
(
    match_id       String,
    participant_id Int32,
    skill_slot     Int32,
    timestamp      Int64
)
ENGINE = PostgreSQL('125.138.61.176:41962', 'lol', 'v_skill_events_flat', 'METAPICK_DB', 'METAPICK_DB');

CREATE TABLE IF NOT EXISTS pg_item_event
(
    id             Int64,
    match_id       String,
    type           String,
    item_id        Int32,
    participant_id Int32,
    timestamp      Int64,
    after_id       Int32,
    before_id      Int32,
    gold_gain      Int32
)
ENGINE = PostgreSQL('125.138.61.176:41962', 'lol', 'item_event', 'METAPICK_DB', 'METAPICK_DB');
