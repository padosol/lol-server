-- platform, patch_version, champion_id, tier_bucket 에 따른 룬 트리
WITH agg AS (
    SELECT individual_position,
        primary_style_id,
        primary_perk0,
        primary_perk1,
        primary_perk2,
        primary_perk3,
        sub_style_id,
        sub_perk0,
        sub_perk1,
        SUM(pick_count) AS pick_count,
        SUM(win_count) AS win_count
    FROM `metapick.lol_analytics.mv_champion_rune_stats`
    WHERE patch_version_int = 1605
        AND platform_id = 'KR'
        AND champion_id = 799
        AND tier_bucket >= 9000 -- DIAMOND+ (>=, BETWEEN, IN 모두 가능)
    GROUP BY individual_position,
        primary_style_id,
        primary_perk0,
        primary_perk1,
        primary_perk2,
        primary_perk3,
        sub_style_id,
        sub_perk0,
        sub_perk1
)
SELECT individual_position,
    ARRAY [primary_perk0, primary_perk1, primary_perk2, primary_perk3] AS primary_perks,
    ARRAY [sub_perk0, sub_perk1] AS secondary_perks,
    pick_count,
    win_count,
    SAFE_DIVIDE(win_count, pick_count) AS win_rate
FROM agg QUALIFY ROW_NUMBER() OVER (
        PARTITION BY individual_position
        ORDER BY pick_count DESC
    ) <= 5
ORDER BY individual_position,
    pick_count DESC;
