-- platform, patch_version, champion_id, tier_bucket 에 따른 15레벨 스킬트리
WITH agg AS (
    SELECT individual_position,
        summoner1id,
        summoner2id,
        SUM(pick_count) AS pick_count,
        SUM(win_count) AS win_count,
        SUM(loss_count) AS loss_count
    FROM `metapick.lol_analytics.mv_champion_spell_stats`
    WHERE patch_version_int = 1607
        AND platform_id = 'KR'
        AND champion_id = 103
        AND tier_bucket >= 6000 -- DIAMOND+ (>=, BETWEEN, IN 자유 변형)
    GROUP BY individual_position,
        summoner1id,
        summoner2id
)
SELECT individual_position,
    summoner1id,
    summoner2id,
    ARRAY [summoner1id, summoner2id] AS spell_ids,
    pick_count,
    win_count,
    loss_count,
    SAFE_DIVIDE(win_count, pick_count) AS win_rate
FROM agg QUALIFY ROW_NUMBER() OVER (
        PARTITION BY individual_position
        ORDER BY pick_count DESC
    ) <= 3
ORDER BY individual_position,
    pick_count DESC;
