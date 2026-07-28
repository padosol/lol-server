-- platform, patch_version, champion_id, tier_bucket 에 따른 스펠
WITH denom AS (
    SELECT SUM(match_count) AS match_count
    FROM `metapick.lol_analytics.mv_match_count_stats`
    WHERE patch_version_int = 1605
        AND platform_id = 'KR'
        AND tier_bucket >= 9000
),
ban_per_champ AS (
    SELECT champion_id,
        SUM(ban_count) AS ban_count
    FROM `metapick.lol_analytics.mv_champion_ban_stats`
    WHERE patch_version_int = 1605
        AND platform_id = 'KR'
        AND tier_bucket >= 9000
    GROUP BY champion_id
),
pick_per_lane AS (
    SELECT individual_position,
        champion_id,
        SUM(pick_count) AS pick_count,
        SUM(win_count) AS win_count
    FROM `metapick.lol_analytics.mv_champion_pick_stats`
    WHERE patch_version_int = 1605
        AND platform_id = 'KR'
        AND tier_bucket >= 9000
    GROUP BY individual_position,
        champion_id
),
base AS (
    SELECT l.individual_position,
        l.champion_id,
        l.pick_count,
        l.win_count,
        COALESCE(b.ban_count, 0) AS ban_count,
        d.match_count,
        SAFE_DIVIDE(l.pick_count, d.match_count) AS pick_rate,
        SAFE_DIVIDE(COALESCE(b.ban_count, 0), d.match_count) AS ban_rate,
        SAFE_DIVIDE(
            l.pick_count + COALESCE(b.ban_count, 0),
            d.match_count
        ) AS presence,
        SAFE_DIVIDE(l.win_count, l.pick_count) AS win_rate,
        -- Wilson score 95% lower bound (z = 1.96)
        SAFE_DIVIDE(
            (l.win_count + 1.96 * 1.96 / 2) / l.pick_count - 1.96 * SQRT(
                SAFE_DIVIDE(
                    l.win_count * (l.pick_count - l.win_count),
                    l.pick_count
                ) + 1.96 * 1.96 / 4
            ) / l.pick_count,
            1 + 1.96 * 1.96 / l.pick_count
        ) AS wilson_wr
    FROM pick_per_lane l
        LEFT JOIN ban_per_champ b USING (champion_id)
        CROSS JOIN denom d
    WHERE l.pick_count >= 20
),
ranked AS (
    SELECT *,
        PERCENT_RANK() OVER (
            PARTITION BY individual_position
            ORDER BY wilson_wr
        ) AS pct_wilson_wr,
        PERCENT_RANK() OVER (
            PARTITION BY individual_position
            ORDER BY presence
        ) AS pct_presence
    FROM base
)
SELECT individual_position,
    champion_id,
    pick_count,
    win_count,
    ban_count,
    match_count,
    ROUND(pick_rate * 100, 2) AS pick_rate_pct,
    ROUND(ban_rate * 100, 2) AS ban_rate_pct,
    ROUND(win_rate * 100, 2) AS win_rate_pct,
    ROUND(wilson_wr * 100, 2) AS wilson_wr_pct,
    ROUND(presence * 100, 2) AS presence_pct,
    ROUND(0.6 * pct_wilson_wr + 0.4 * pct_presence, 4) AS tier_score,
    CASE
        WHEN 0.6 * pct_wilson_wr + 0.4 * pct_presence >= 0.97 THEN 'S+'
        WHEN 0.6 * pct_wilson_wr + 0.4 * pct_presence >= 0.90 THEN 'S'
        WHEN 0.6 * pct_wilson_wr + 0.4 * pct_presence >= 0.75 THEN 'A'
        WHEN 0.6 * pct_wilson_wr + 0.4 * pct_presence >= 0.50 THEN 'B'
        WHEN 0.6 * pct_wilson_wr + 0.4 * pct_presence >= 0.20 THEN 'C'
        ELSE 'D'
    END AS tier
FROM ranked
ORDER BY individual_position,
    tier_score DESC;
