package com.example.lolserver.repository.championstats.adapter;

final class ChampionStatsBigQuerySqls {

    static final String WIN_RATES = """
            SELECT individual_position                                       AS team_position,
                   CAST(SUM(pick_count) AS INT64)                            AS total_games,
                   CAST(SUM(win_count) AS INT64)                             AS total_wins,
                   COALESCE(ROUND(SAFE_DIVIDE(SUM(win_count), SUM(pick_count)), 4), 0) AS total_win_rate
            FROM %s
            WHERE champion_id = @championId
              AND patch_version_int = @patch
              AND platform_id = @platform
              AND tier_bucket IN UNNEST(@tierBuckets)
            GROUP BY individual_position
            ORDER BY total_games DESC
            """;

    static final String MATCHUPS = """
            WITH
                agg AS (
                    SELECT matchup_champion_id,
                           SUM(pick_count) AS pick_count,
                           SUM(win_count)  AS win_count
                    FROM %s
                    WHERE patch_version_int = @patch
                      AND platform_id = @platform
                      AND tier_bucket IN UNNEST(@tierBuckets)
                      AND champion_id = @championId
                      AND individual_position = @position
                    GROUP BY matchup_champion_id
                    HAVING pick_count >= 30
                ),
                total AS (SELECT SUM(pick_count) AS total_games FROM agg)
            SELECT a.matchup_champion_id                  AS opponent_champion_id,
                   a.pick_count                           AS games,
                   SAFE_DIVIDE(a.win_count, a.pick_count) AS win_rate,
                   SAFE_DIVIDE(a.pick_count, t.total_games) AS pick_rate
            FROM agg AS a
            CROSS JOIN total AS t
            ORDER BY win_rate %s, a.pick_count DESC
            LIMIT 3
            """;

    static final String RUNE_BUILDS = """
            WITH
                agg AS (
                    SELECT primary_style_id, sub_style_id,
                           primary_perk0, primary_perk1, primary_perk2, primary_perk3,
                           sub_perk0, sub_perk1,
                           SUM(pick_count) AS pick_count,
                           SUM(win_count)  AS win_count
                    FROM %s
                    WHERE patch_version_int = @patch
                      AND platform_id = @platform
                      AND tier_bucket IN UNNEST(@tierBuckets)
                      AND champion_id = @championId
                      AND individual_position = @position
                    GROUP BY primary_style_id, sub_style_id,
                             primary_perk0, primary_perk1, primary_perk2, primary_perk3,
                             sub_perk0, sub_perk1
                ),
                total AS (SELECT SUM(pick_count) AS total_games FROM agg)
            SELECT a.primary_style_id, a.sub_style_id,
                   a.primary_perk0, a.primary_perk1, a.primary_perk2, a.primary_perk3,
                   a.sub_perk0, a.sub_perk1,
                   a.pick_count                           AS games,
                   SAFE_DIVIDE(a.win_count, a.pick_count) AS win_rate,
                   SAFE_DIVIDE(a.pick_count, t.total_games) AS pick_rate
            FROM agg AS a
            CROSS JOIN total AS t
            ORDER BY a.pick_count DESC
            LIMIT 2
            """;

    static final String SPELL_STATS = """
            WITH
                agg AS (
                    SELECT summoner1id, summoner2id,
                           SUM(pick_count) AS pick_count,
                           SUM(win_count)  AS win_count
                    FROM %s
                    WHERE patch_version_int = @patch
                      AND platform_id = @platform
                      AND tier_bucket IN UNNEST(@tierBuckets)
                      AND champion_id = @championId
                      AND individual_position = @position
                    GROUP BY summoner1id, summoner2id
                    HAVING pick_count >= 30
                ),
                total AS (SELECT SUM(pick_count) AS total_games FROM agg)
            SELECT a.summoner1id, a.summoner2id,
                   a.pick_count                           AS games,
                   SAFE_DIVIDE(a.win_count, a.pick_count) AS win_rate,
                   SAFE_DIVIDE(a.pick_count, t.total_games) AS pick_rate
            FROM agg AS a
            CROSS JOIN total AS t
            ORDER BY a.pick_count DESC
            LIMIT 2
            """;

    static final String SKILL_BUILDS = """
            WITH
                agg AS (
                    SELECT skill1, skill2, skill3, skill4, skill5,
                           skill6, skill7, skill8, skill9, skill10,
                           skill11, skill12, skill13, skill14, skill15,
                           SUM(pick_count) AS pick_count,
                           SUM(win_count)  AS win_count
                    FROM %s
                    WHERE patch_version_int = @patch
                      AND platform_id = @platform
                      AND tier_bucket IN UNNEST(@tierBuckets)
                      AND champion_id = @championId
                      AND individual_position = @position
                    GROUP BY skill1, skill2, skill3, skill4, skill5,
                             skill6, skill7, skill8, skill9, skill10,
                             skill11, skill12, skill13, skill14, skill15
                ),
                total AS (SELECT SUM(pick_count) AS total_games FROM agg)
            SELECT TO_JSON_STRING([
                       a.skill1, a.skill2, a.skill3, a.skill4, a.skill5,
                       a.skill6, a.skill7, a.skill8, a.skill9, a.skill10,
                       a.skill11, a.skill12, a.skill13, a.skill14, a.skill15
                   ])                                       AS skill_build,
                   a.pick_count                             AS games,
                   SAFE_DIVIDE(a.win_count, a.pick_count)   AS win_rate,
                   SAFE_DIVIDE(a.pick_count, t.total_games) AS pick_rate
            FROM agg AS a
            CROSS JOIN total AS t
            ORDER BY a.pick_count DESC
            LIMIT 2
            """;

    static final String START_ITEM_BUILDS = """
            WITH
                agg AS (
                    SELECT start_item_ids_json,
                           SUM(pick_count) AS pick_count,
                           SUM(win_count)  AS win_count
                    FROM %s
                    WHERE patch_version_int = @patch
                      AND platform_id = @platform
                      AND tier_bucket IN UNNEST(@tierBuckets)
                      AND champion_id = @championId
                      AND individual_position = @position
                    GROUP BY start_item_ids_json
                    HAVING pick_count >= 30
                ),
                total AS (SELECT SUM(pick_count) AS total_games FROM agg)
            SELECT a.start_item_ids_json                  AS start_items,
                   a.pick_count                           AS games,
                   SAFE_DIVIDE(a.win_count, a.pick_count) AS win_rate,
                   SAFE_DIVIDE(a.pick_count, t.total_games) AS pick_rate
            FROM agg AS a
            CROSS JOIN total AS t
            ORDER BY a.pick_count DESC
            LIMIT 2
            """;

    static final String BOOT_BUILDS = """
            WITH
                agg AS (
                    SELECT boot_id,
                           SUM(pick_count) AS pick_count,
                           SUM(win_count)  AS win_count
                    FROM %s
                    WHERE patch_version_int = @patch
                      AND platform_id = @platform
                      AND tier_bucket IN UNNEST(@tierBuckets)
                      AND champion_id = @championId
                      AND individual_position = @position
                    GROUP BY boot_id
                    HAVING pick_count >= 30
                ),
                total AS (SELECT SUM(pick_count) AS total_games FROM agg)
            SELECT a.boot_id,
                   a.pick_count                           AS games,
                   SAFE_DIVIDE(a.win_count, a.pick_count) AS win_rate,
                   SAFE_DIVIDE(a.pick_count, t.total_games) AS pick_rate
            FROM agg AS a
            CROSS JOIN total AS t
            ORDER BY a.pick_count DESC
            LIMIT 2
            """;

    static final String ITEM_BUILDS = """
            WITH
                agg AS (
                    SELECT item1, item2, item3,
                           SUM(pick_count) AS pick_count,
                           SUM(win_count)  AS win_count
                    FROM %s
                    WHERE patch_version_int = @patch
                      AND platform_id = @platform
                      AND tier_bucket IN UNNEST(@tierBuckets)
                      AND champion_id = @championId
                      AND individual_position = @position
                    GROUP BY item1, item2, item3
                    HAVING pick_count >= 30
                ),
                total AS (SELECT SUM(pick_count) AS total_games FROM agg)
            SELECT TO_JSON_STRING([a.item1, a.item2, a.item3]) AS item_build,
                   a.pick_count                           AS games,
                   SAFE_DIVIDE(a.win_count, a.pick_count) AS win_rate,
                   SAFE_DIVIDE(a.pick_count, t.total_games) AS pick_rate
            FROM agg AS a
            CROSS JOIN total AS t
            ORDER BY a.pick_count DESC
            LIMIT 2
            """;

    static final String STATS_BY_POSITION = """
            WITH
                denom AS (
                    SELECT SUM(match_count) AS match_count
                    FROM %s
                    WHERE patch_version_int = @patch
                      AND platform_id = @platform
                      AND tier_bucket IN UNNEST(@tierBuckets)
                ),
                ban_per_champ AS (
                    SELECT champion_id,
                           SUM(ban_count) AS ban_count
                    FROM %s
                    WHERE patch_version_int = @patch
                      AND platform_id = @platform
                      AND tier_bucket IN UNNEST(@tierBuckets)
                    GROUP BY champion_id
                ),
                pick_per_lane AS (
                    SELECT individual_position,
                           champion_id,
                           SUM(pick_count) AS pick_count,
                           SUM(win_count)  AS win_count
                    FROM %s
                    WHERE patch_version_int = @patch
                      AND platform_id = @platform
                      AND tier_bucket IN UNNEST(@tierBuckets)
                    GROUP BY individual_position, champion_id
                )
            SELECT l.individual_position                                                       AS team_position,
                   l.champion_id                                                               AS champion_id,
                   COALESCE(ROUND(SAFE_DIVIDE(l.win_count, l.pick_count), 4), 0)               AS win_rate,
                   COALESCE(ROUND(SAFE_DIVIDE(l.pick_count, d.match_count), 4), 0)             AS pick_rate,
                   COALESCE(ROUND(SAFE_DIVIDE(COALESCE(b.ban_count, 0), d.match_count), 4), 0) AS ban_rate,
                   l.pick_count                                                                AS total_games
            FROM pick_per_lane AS l
            CROSS JOIN denom AS d
            LEFT JOIN ban_per_champ AS b USING (champion_id)
            WHERE l.pick_count >= 20
            ORDER BY l.individual_position, l.pick_count DESC
            """;

    private ChampionStatsBigQuerySqls() {
    }
}
