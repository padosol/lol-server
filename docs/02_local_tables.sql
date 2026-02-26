-- =====================================================
-- ClickHouse: 분석용 로컬 테이블 DDL
-- =====================================================
-- 비정규화된 팩트 테이블 (ReplacingMergeTree)
-- 랭크 솔로 큐 (queueId = 420) 전용
-- =====================================================

-- -----------------------------------------------------
-- 2-1. match_participant_local — 핵심 팩트 테이블
-- -----------------------------------------------------
-- 매치당 참가자 1행. 아이템/스킬/룬 데이터를 모두 포함하는 비정규화 테이블.
CREATE TABLE IF NOT EXISTS match_participant_local
(
    -- 매치 식별
    match_id          String,
    patch             LowCardinality(String),
    platform_id       LowCardinality(String),
    queue_id          UInt16,

    -- 참가자 식별
    participant_id    UInt8,
    team_id           UInt16,
    puuid             String,

    -- 챔피언 정보
    champion_id       UInt16,
    champion_name     LowCardinality(String),
    team_position     LowCardinality(String),

    -- 결과
    win               UInt8,

    -- 티어 (매치 저장 시점 스냅샷)
    tier              LowCardinality(String),
    tier_rank         LowCardinality(String),
    absolute_points   Nullable(UInt16),

    -- 최종 아이템
    item0             UInt16,
    item1             UInt16,
    item2             UInt16,
    item3             UInt16,
    item4             UInt16,
    item5             UInt16,
    item6             UInt16,

    -- 아이템 빌드 순서 (item_events에서 추출)
    item_build_order  Array(UInt16),

    -- 스킬 빌드 순서 (skill_events에서 추출, 1=Q, 2=W, 3=E, 4=R)
    skill_order       Array(UInt8),

    -- 룬
    primary_style_id  UInt16,
    primary_perk_ids  Array(UInt16),
    sub_style_id      UInt16,
    sub_perk_ids      Array(UInt16),

    -- 소환사 주문
    summoner1_id      UInt16,
    summoner2_id      UInt16
)
ENGINE = ReplacingMergeTree()
PARTITION BY patch
ORDER BY (champion_id, team_position, tier, match_id, participant_id)
SETTINGS index_granularity = 8192;

-- -----------------------------------------------------
-- 2-2. match_lane_matchup_local — 라인 매치업 테이블
-- -----------------------------------------------------
-- 같은 라인 상대를 사전 매칭. 양방향 2행 (A vs B, B vs A).
CREATE TABLE IF NOT EXISTS match_lane_matchup_local
(
    match_id                String,
    patch                   LowCardinality(String),
    platform_id             LowCardinality(String),

    champion_id             UInt16,
    champion_name           LowCardinality(String),
    team_position           LowCardinality(String),
    tier                    LowCardinality(String),
    win                     UInt8,

    opponent_champion_id    UInt16,
    opponent_champion_name  LowCardinality(String)
)
ENGINE = ReplacingMergeTree()
PARTITION BY patch
ORDER BY (champion_id, opponent_champion_id, team_position, tier, platform_id, match_id)
SETTINGS index_granularity = 8192;
