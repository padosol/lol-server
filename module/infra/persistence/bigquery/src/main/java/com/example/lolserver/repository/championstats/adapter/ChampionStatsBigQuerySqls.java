package com.example.lolserver.repository.championstats.adapter;

final class ChampionStatsBigQuerySqls {

    static final String WIN_RATES = """
            SELECT team_position                              AS team_position,
                   CAST(COUNT(*) AS INT64)                    AS total_games,
                   CAST(SUM(win) AS INT64)                    AS total_wins,
                   COALESCE(ROUND(SUM(win) / NULLIF(COUNT(*), 0), 4), 0) AS total_win_rate
            FROM %s
            WHERE champion_id = @championId
              AND patch_version = @patch
              AND platform_id = @platform
              AND tier IN UNNEST(@tiers)
            GROUP BY team_position
            ORDER BY total_games DESC
            """;

    static final String MATCHUPS = """
            WITH
                matchup_stats AS (
                    SELECT opponent_champion_id,
                           SUM(games) AS games,
                           SUM(wins)  AS wins
                    FROM %s
                    WHERE patch_version = @patch
                      AND platform_id = @platform
                      AND tier IN UNNEST(@tiers)
                      AND champion_id = @championId
                      AND team_position = @position
                    GROUP BY opponent_champion_id
                    HAVING games >= 50
                ),
                total AS (SELECT SUM(games) AS total_games FROM matchup_stats)
            SELECT ms.opponent_champion_id,
                   ms.games,
                   ms.wins / ms.games       AS win_rate,
                   ms.games / t.total_games AS pick_rate
            FROM matchup_stats AS ms
            CROSS JOIN total AS t
            ORDER BY win_rate %s
            LIMIT 3
            """;

    static final String RUNE_BUILDS = """
            WITH
                rune_stats AS (
                    SELECT primary_style_id, sub_style_id,
                           primary_perk0, primary_perk1, primary_perk2, primary_perk3,
                           sub_perk0, sub_perk1,
                           stat_perk_defense, stat_perk_flex, stat_perk_offense,
                           SUM(games) AS games,
                           SUM(wins)  AS wins
                    FROM %s
                    WHERE patch_version = @patch
                      AND platform_id = @platform
                      AND tier IN UNNEST(@tiers)
                      AND champion_id = @championId
                      AND team_position = @position
                    GROUP BY primary_style_id, sub_style_id,
                             primary_perk0, primary_perk1, primary_perk2, primary_perk3,
                             sub_perk0, sub_perk1,
                             stat_perk_defense, stat_perk_flex, stat_perk_offense
                ),
                total AS (SELECT SUM(games) AS total_games FROM rune_stats)
            SELECT rs.primary_style_id, rs.sub_style_id,
                   rs.primary_perk0, rs.primary_perk1, rs.primary_perk2, rs.primary_perk3,
                   rs.sub_perk0, rs.sub_perk1,
                   rs.stat_perk_defense, rs.stat_perk_flex, rs.stat_perk_offense,
                   rs.games,
                   rs.wins / rs.games       AS win_rate,
                   rs.games / t.total_games AS pick_rate
            FROM rune_stats AS rs
            CROSS JOIN total AS t
            ORDER BY rs.games DESC
            LIMIT 2
            """;

    static final String SPELL_STATS = """
            WITH
                spell_stats AS (
                    SELECT summoner1id, summoner2id,
                           SUM(games) AS games,
                           SUM(wins)  AS wins
                    FROM %s
                    WHERE patch_version = @patch
                      AND platform_id = @platform
                      AND tier IN UNNEST(@tiers)
                      AND champion_id = @championId
                      AND team_position = @position
                    GROUP BY summoner1id, summoner2id
                ),
                total AS (SELECT SUM(games) AS total_games FROM spell_stats)
            SELECT ss.summoner1id,
                   ss.summoner2id,
                   ss.games,
                   ss.wins / ss.games       AS win_rate,
                   ss.games / t.total_games AS pick_rate
            FROM spell_stats AS ss
            CROSS JOIN total AS t
            ORDER BY ss.games DESC
            LIMIT 2
            """;

    static final String SKILL_BUILDS = """
            WITH
                skill_stats AS (
                    SELECT skill_build,
                           SUM(games) AS games,
                           SUM(wins)  AS wins
                    FROM %s
                    WHERE patch_version = @patch
                      AND platform_id = @platform
                      AND tier IN UNNEST(@tiers)
                      AND champion_id = @championId
                      AND team_position = @position
                    GROUP BY skill_build
                ),
                total AS (SELECT SUM(games) AS total_games FROM skill_stats)
            SELECT sk.skill_build,
                   sk.games,
                   sk.wins / sk.games       AS win_rate,
                   sk.games / t.total_games AS pick_rate
            FROM skill_stats AS sk
            CROSS JOIN total AS t
            ORDER BY sk.games DESC
            LIMIT 2
            """;

    static final String START_ITEM_BUILDS = """
            WITH
                item_stats AS (
                    SELECT start_items,
                           SUM(games) AS games,
                           SUM(wins)  AS wins
                    FROM %s
                    WHERE patch_version = @patch
                      AND platform_id = @platform
                      AND tier IN UNNEST(@tiers)
                      AND champion_id = @championId
                      AND team_position = @position
                    GROUP BY start_items
                ),
                total AS (SELECT SUM(games) AS total_games FROM item_stats)
            SELECT its.start_items,
                   its.games,
                   its.wins / its.games       AS win_rate,
                   its.games / t.total_games  AS pick_rate
            FROM item_stats AS its
            CROSS JOIN total AS t
            ORDER BY its.games DESC
            LIMIT 2
            """;

    static final String ITEM_BUILDS = """
            WITH
                build_stats AS (
                    SELECT item_build, SUM(games) AS games, SUM(wins) AS wins
                    FROM %s
                    WHERE patch_version = @patch
                      AND platform_id = @platform
                      AND tier IN UNNEST(@tiers)
                      AND champion_id = @championId
                      AND team_position = @position
                    GROUP BY item_build
                ),
                total AS (SELECT SUM(games) AS total_games FROM build_stats)
            SELECT bs.item_build,
                   bs.games,
                   bs.wins / bs.games       AS win_rate,
                   bs.games / t.total_games AS pick_rate
            FROM build_stats AS bs
            CROSS JOIN total AS t
            ORDER BY bs.games DESC
            LIMIT 2
            """;

    static final String ITEM_STATS = """
            WITH
                item_stats AS (
                    SELECT item_id, SUM(games) AS games, SUM(wins) AS wins
                    FROM %s
                    WHERE patch_version = @patch
                      AND platform_id = @platform
                      AND tier IN UNNEST(@tiers)
                      AND champion_id = @championId
                      AND team_position = @position
                      AND item_order = @itemOrder
                    GROUP BY item_id
                ),
                total AS (
                    SELECT SUM(games) AS total_games
                    FROM %s
                    WHERE patch_version = @patch
                      AND platform_id = @platform
                      AND tier IN UNNEST(@tiers)
                      AND champion_id = @championId
                      AND team_position = @position
                )
            SELECT its.item_id              AS item_id,
                   li.item_name             AS item_name,
                   its.games                AS games,
                   its.wins / its.games     AS win_rate,
                   its.games / t.total_games AS pick_rate
            FROM item_stats AS its
            INNER JOIN %s AS li USING (item_id)
            CROSS JOIN total AS t
            ORDER BY its.games DESC
            LIMIT 2
            """;

    static final String STATS_BY_POSITION = """
            WITH
                stats AS (
                    SELECT champion_id, team_position,
                           SUM(games) AS games, SUM(wins) AS wins
                    FROM %s
                    WHERE patch_version = @patch
                      AND platform_id = @platform
                      AND tier IN UNNEST(@tiers)
                    GROUP BY champion_id, team_position
                ),
                pick_total AS (
                    SELECT team_position, SUM(participant_rows) AS participant_rows
                    FROM %s
                    WHERE patch_version = @patch
                      AND platform_id = @platform
                      AND tier IN UNNEST(@tiers)
                    GROUP BY team_position
                ),
                ban_stats AS (
                    SELECT champion_id, SUM(bans) AS bans
                    FROM %s
                    WHERE patch_version = @patch
                      AND platform_id = @platform
                      AND tier IN UNNEST(@tiers)
                    GROUP BY champion_id
                ),
                ban_total AS (
                    SELECT SUM(participant_rows) AS total_participants
                    FROM %s
                    WHERE patch_version = @patch
                      AND platform_id = @platform
                      AND tier IN UNNEST(@tiers)
                )
            SELECT s.team_position AS team_position, s.champion_id AS champion_id,
                   COALESCE(ROUND(s.wins / NULLIF(s.games, 0), 4), 0) AS win_rate,
                   COALESCE(ROUND(s.games / NULLIF(pt.participant_rows, 0), 4), 0) AS pick_rate,
                   COALESCE(ROUND(COALESCE(b.bans, 0) / NULLIF(bt.total_participants, 0), 4), 0) AS ban_rate,
                   s.games AS total_games
            FROM stats AS s
            INNER JOIN pick_total AS pt ON s.team_position = pt.team_position
            LEFT  JOIN ban_stats  AS b  ON s.champion_id   = b.champion_id
            CROSS JOIN ban_total  AS bt
            ORDER BY s.team_position, s.games DESC
            """;

    private ChampionStatsBigQuerySqls() {
    }
}
