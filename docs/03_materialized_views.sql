-- =====================================================
-- ClickHouse: Materialized View + 집계 대상 테이블
-- =====================================================
-- SummingMergeTree 엔진으로 자동 사전 집계
-- match_participant_local / match_lane_matchup_local INSERT 시 자동 트리거
-- =====================================================

-- -----------------------------------------------------
-- 4-1. 챔피언 기본 통계
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS champion_stats_local
(
    champion_id       UInt16,
    team_position     LowCardinality(String),
    tier              LowCardinality(String),
    patch             LowCardinality(String),
    platform_id       LowCardinality(String),
    games             UInt64,
    wins              UInt64
)
ENGINE = SummingMergeTree()
ORDER BY (champion_id, team_position, tier, patch, platform_id);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_champion_stats_local
TO champion_stats_local
AS SELECT
    champion_id, team_position, tier, patch, platform_id,
    1                                AS games,
    toUInt64(win)                    AS wins
FROM match_participant_local;

-- -----------------------------------------------------
-- 4-2. 아이템 빌드 통계
-- -----------------------------------------------------
-- 최종 아이템 조합 (item0~item5, 장신구 제외) 기준 빈도 및 승률
CREATE TABLE IF NOT EXISTS item_build_stats_local
(
    champion_id       UInt16,
    team_position     LowCardinality(String),
    tier              LowCardinality(String),
    patch             LowCardinality(String),
    platform_id       LowCardinality(String),
    items_sorted      Array(UInt16),
    games             UInt64,
    wins              UInt64
)
ENGINE = SummingMergeTree()
ORDER BY (champion_id, team_position, tier, patch, platform_id, items_sorted);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_item_build_local
TO item_build_stats_local
AS SELECT
    champion_id, team_position, tier, patch, platform_id,
    arraySort(arrayFilter(x -> x != 0, [item0, item1, item2, item3, item4, item5])) AS items_sorted,
    1                 AS games,
    toUInt64(win)     AS wins
FROM match_participant_local;

-- -----------------------------------------------------
-- 4-3. 스킬 빌드 통계
-- -----------------------------------------------------
-- 스킬 레벨업 순서 (처음 15레벨) 기준 빈도 및 승률
CREATE TABLE IF NOT EXISTS skill_build_stats_local
(
    champion_id       UInt16,
    team_position     LowCardinality(String),
    tier              LowCardinality(String),
    patch             LowCardinality(String),
    platform_id       LowCardinality(String),
    skill_order_15    Array(UInt8),
    games             UInt64,
    wins              UInt64
)
ENGINE = SummingMergeTree()
ORDER BY (champion_id, team_position, tier, patch, platform_id, skill_order_15);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_skill_build_local
TO skill_build_stats_local
AS SELECT
    champion_id, team_position, tier, patch, platform_id,
    arraySlice(skill_order, 1, 15)  AS skill_order_15,
    1                               AS games,
    toUInt64(win)                   AS wins
FROM match_participant_local;

-- -----------------------------------------------------
-- 4-4. 룬 빌드 통계
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS rune_build_stats_local
(
    champion_id       UInt16,
    team_position     LowCardinality(String),
    tier              LowCardinality(String),
    patch             LowCardinality(String),
    platform_id       LowCardinality(String),
    primary_style_id  UInt16,
    primary_perk_ids  Array(UInt16),
    sub_style_id      UInt16,
    sub_perk_ids      Array(UInt16),
    games             UInt64,
    wins              UInt64
)
ENGINE = SummingMergeTree()
ORDER BY (champion_id, team_position, tier, patch, platform_id,
          primary_style_id, primary_perk_ids, sub_style_id, sub_perk_ids);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_rune_build_local
TO rune_build_stats_local
AS SELECT
    champion_id, team_position, tier, patch, platform_id,
    primary_style_id, primary_perk_ids, sub_style_id, sub_perk_ids,
    1                 AS games,
    toUInt64(win)     AS wins
FROM match_participant_local;

-- -----------------------------------------------------
-- 4-5. 챔피언 매치업 통계
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS champion_matchup_stats_local
(
    champion_id              UInt16,
    opponent_champion_id     UInt16,
    team_position            LowCardinality(String),
    tier                     LowCardinality(String),
    patch                    LowCardinality(String),
    platform_id              LowCardinality(String),
    games                    UInt64,
    wins                     UInt64
)
ENGINE = SummingMergeTree()
ORDER BY (champion_id, opponent_champion_id, team_position, tier, patch, platform_id);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_champion_matchup_local
TO champion_matchup_stats_local
AS SELECT
    champion_id, opponent_champion_id, team_position, tier, patch, platform_id,
    1                                        AS games,
    toUInt64(win)                            AS wins
FROM match_lane_matchup_local;
