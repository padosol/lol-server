-- platform, patch_version, champion_id, tier_bucket 에 따른 시작 아이템
WITH agg AS (
    SELECT individual_position,
        boot_id,
        SUM(pick_count) AS pick_count,
        SUM(win_count) AS win_count,
        SUM(loss_count) AS loss_count
    FROM `metapick.lol_analytics.mv_champion_boot_stats`
    WHERE patch_version_int = 1605
        AND platform_id = 'KR'
        AND champion_id = 64
        AND tier_bucket >= 9000 -- DIAMOND 이상
    GROUP BY individual_position,
        boot_id
)
SELECT individual_position,
    boot_id,
    pick_count,
    win_count,
    loss_count,
    SAFE_DIVIDE(win_count, pick_count) AS win_rate
FROM agg
WHERE pick_count >= 30 QUALIFY ROW_NUMBER() OVER (
        PARTITION BY individual_position
        ORDER BY pick_count DESC
    ) <= 5
ORDER BY individual_position,
    pick_count DESC;
