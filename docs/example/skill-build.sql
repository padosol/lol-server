-- ─────────────────────────────────────────────────────────────
-- [2] 라인별 스킬 빌드 Top 3 — 티어 범위 합산
-- 한 챔피언의 모든 라인 스킬 메타를 한 번에.
-- ─────────────────────────────────────────────────────────────
WITH agg AS (
    SELECT individual_position,
        skill1,
        skill2,
        skill3,
        skill4,
        skill5,
        skill6,
        skill7,
        skill8,
        skill9,
        skill10,
        skill11,
        skill12,
        skill13,
        skill14,
        skill15,
        SUM(pick_count) AS pick_count,
        SUM(win_count) AS win_count,
        SUM(loss_count) AS loss_count
    FROM `metapick.lol_analytics.mv_champion_skill_stats`
    WHERE patch_version_int = 1607
        AND platform_id = 'KR'
        AND champion_id = 103
        AND tier_bucket >= 6000
    GROUP BY individual_position,
        skill1,
        skill2,
        skill3,
        skill4,
        skill5,
        skill6,
        skill7,
        skill8,
        skill9,
        skill10,
        skill11,
        skill12,
        skill13,
        skill14,
        skill15
)
SELECT individual_position,
    skill1,
    skill2,
    skill3,
    skill4,
    skill5,
    skill6,
    skill7,
    skill8,
    skill9,
    skill10,
    skill11,
    skill12,
    skill13,
    skill14,
    skill15,
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