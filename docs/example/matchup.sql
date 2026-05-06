-- platform, patch_version, champion_id, tier_bucket 에 따른 매치업
WITH agg AS (
    SELECT individual_position,
        matchup_champion_id,
        SUM(pick_count) AS pick_count,
        SUM(win_count) AS win_count
    FROM `metapick.lol_analytics.mv_champion_matchup_stats`
    WHERE patch_version_int = 1607
        AND platform_id = 'KR'
        AND champion_id = 64
        AND tier_bucket >= 9000
    GROUP BY individual_position,
        matchup_champion_id
) -- 잘 잡는 상대 Top 5 (승률 높은 순)
SELECT 'TOP' AS rank_type,
    individual_position,
    matchup_champion_id,
    pick_count,
    win_count,
    SAFE_DIVIDE(win_count, pick_count) AS win_rate
FROM agg
WHERE pick_count >= 30 QUALIFY ROW_NUMBER() OVER (
        PARTITION BY individual_position
        ORDER BY SAFE_DIVIDE(win_count, pick_count) DESC,
            pick_count DESC
    ) <= 5
UNION ALL
-- 카운터 Top 5 (승률 낮은 순)
SELECT 'BOTTOM' AS rank_type,
    individual_position,
    matchup_champion_id,
    pick_count,
    win_count,
    SAFE_DIVIDE(win_count, pick_count) AS win_rate
FROM agg
WHERE pick_count >= 30 QUALIFY ROW_NUMBER() OVER (
        PARTITION BY individual_position
        ORDER BY SAFE_DIVIDE(win_count, pick_count) ASC,
            pick_count DESC
    ) <= 5
ORDER BY individual_position,
    CASE
        rank_type
        WHEN 'TOP' THEN 0
        WHEN 'BOTTOM' THEN 1
    END,
    -- TOP 블록 먼저
    CASE
        rank_type
        WHEN 'TOP' THEN win_rate
    END DESC,
    -- TOP 안에선 높은 순
    CASE
        rank_type
        WHEN 'BOTTOM' THEN win_rate
    END ASC;
-- BOTTOM 안에선 낮은 순
