package com.example.lolserver.repository.championstats.adapter;

import com.example.lolserver.TierFilter;
import com.example.lolserver.domain.championstats.application.model.ChampionBootBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionMatchupReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRuneBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSkillBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSpellStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionStartItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRateReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionWinRateReadModel;
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsQueryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChampionStatsClickHouseAdapter implements ChampionStatsQueryPort {

    private final JdbcTemplate clickHouseJdbcTemplate;

    public ChampionStatsClickHouseAdapter(
            @Qualifier("clickHouseJdbcTemplate") JdbcTemplate clickHouseJdbcTemplate) {
        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
    }

    private static String quote(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static List<Integer> parseCsvIntArray(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();
    }

    /**
     * TierFilter의 티어 이름들을 SQL IN 절로 변환합니다.
     *
     * <p>
     * SQL Injection 안전성: TierFilter.of()가 Tier.valueOf()로 검증하므로
     * tierNames에는 Tier enum에 정의된 값만 포함됩니다.
     * ClickHouse JDBC는 IN 절의 PreparedStatement 바인딩을 지원하지 않아
     * 문자열 삽입 방식을 사용하되, quote()로 이스케이프합니다.
     */
    private static String tierInClause(TierFilter tierFilter) {
        return tierFilter.getTierNames().stream()
                .map(ChampionStatsClickHouseAdapter::quote)
                .collect(Collectors.joining(", ", "tier IN (", ")"));
    }

    @Override
    public List<ChampionWinRateReadModel> getChampionWinRates(
            int championId, String patch, String platformId, TierFilter tierFilter) {
        log.info("championId: {}, patch: {}, platformId: {}, tierFilter: {}",
                championId, patch, platformId, tierFilter);
        String sql = """
                SELECT team_position       AS team_position,
                       toInt64(count(*))   AS total_games,
                       toInt64(sum(win))   AS total_wins,
                       coalesce(round(sum(win) / nullIf(count(*), 0), 4), 0) AS total_win_rate
                FROM match_participant_local
                WHERE champion_id = %d AND patch_version = %s AND platform_id = %s AND %s
                GROUP BY team_position
                HAVING count(*) > 20
                ORDER BY total_games DESC
                """.formatted(championId, quote(patch), quote(platformId), tierInClause(tierFilter));

        return clickHouseJdbcTemplate.query(sql,
                (rs, rowNum) -> new ChampionWinRateReadModel(
                        rs.getString("team_position"),
                        rs.getLong("total_games"),
                        rs.getLong("total_wins"),
                        rs.getDouble("total_win_rate")));
    }

    @Override
    public List<ChampionMatchupReadModel> getChampionMatchups(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        List<ChampionMatchupReadModel> matchups = new ArrayList<>();
        matchups.addAll(queryMatchups(championId, patch, platformId, tierFilter, position, "TOP", "DESC"));
        matchups.addAll(queryMatchups(championId, patch, platformId, tierFilter, position, "BOTTOM", "ASC"));
        return matchups;
    }

    private List<ChampionMatchupReadModel> queryMatchups(
            int championId, String patch, String platformId,
            TierFilter tierFilter, String position, String rankType, String orderDirection) {
        String sql = """
                WITH
                    matchup_stats AS (
                        SELECT
                            opponent_champion_id,
                            sum(games) AS games,
                            sum(wins)  AS wins
                        FROM champion_matchup_stats_agg
                        WHERE patch_version = %1$s AND platform_id = %2$s AND %3$s
                              AND champion_id = %4$d AND team_position = %5$s
                        GROUP BY opponent_champion_id
                        HAVING games >= 50
                    ),
                    total AS (
                        SELECT sum(games) AS total_games FROM matchup_stats
                    )
                SELECT
                    ms.opponent_champion_id,
                    ms.games,
                    ms.wins / ms.games       AS win_rate,
                    ms.games / t.total_games AS pick_rate
                FROM matchup_stats AS ms
                CROSS JOIN total AS t
                ORDER BY win_rate %6$s
                LIMIT 5
                """.formatted(
                quote(patch), quote(platformId), tierInClause(tierFilter),
                championId, quote(position), orderDirection);

        return clickHouseJdbcTemplate.query(sql,
                (rs, rowNum) -> new ChampionMatchupReadModel(
                        rankType,
                        rs.getInt("opponent_champion_id"),
                        rs.getLong("games"),
                        rs.getDouble("win_rate"),
                        rs.getDouble("pick_rate")));
    }

    @Override
    public List<ChampionRuneBuildReadModel> getChampionRuneBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        String sql = """
                WITH
                    rune_stats AS (
                        SELECT
                            primary_style_id, sub_style_id,
                            primary_perk0, primary_perk1, primary_perk2, primary_perk3,
                            sub_perk0, sub_perk1,
                            sum(games) AS games,
                            sum(wins)  AS wins
                        FROM champion_rune_stats_agg
                        WHERE patch_version = %1$s AND platform_id = %2$s AND %3$s
                              AND champion_id = %4$d AND team_position = %5$s
                        GROUP BY primary_style_id, sub_style_id,
                                 primary_perk0, primary_perk1, primary_perk2, primary_perk3,
                                 sub_perk0, sub_perk1
                    ),
                    total AS (
                        SELECT sum(games) AS total_games FROM rune_stats
                    )
                SELECT
                    rs.primary_style_id, rs.sub_style_id,
                    rs.primary_perk0, rs.primary_perk1, rs.primary_perk2, rs.primary_perk3,
                    rs.sub_perk0, rs.sub_perk1,
                    rs.games,
                    rs.wins / rs.games       AS win_rate,
                    rs.games / t.total_games AS pick_rate
                FROM rune_stats AS rs
                CROSS JOIN total AS t
                ORDER BY rs.games DESC
                LIMIT 2
                """.formatted(quote(patch), quote(platformId), tierInClause(tierFilter), championId, quote(position));

        return clickHouseJdbcTemplate.query(sql,
                (rs, rowNum) -> new ChampionRuneBuildReadModel(
                        rs.getInt("primary_style_id"),
                        rs.getInt("sub_style_id"),
                        rs.getInt("primary_perk0"),
                        rs.getInt("primary_perk1"),
                        rs.getInt("primary_perk2"),
                        rs.getInt("primary_perk3"),
                        rs.getInt("sub_perk0"),
                        rs.getInt("sub_perk1"),
                        rs.getLong("games"),
                        rs.getDouble("win_rate"),
                        rs.getDouble("pick_rate")));
    }

    @Override
    public List<ChampionSpellStatsReadModel> getChampionSpellStats(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        String sql = """
                WITH
                    spell_stats AS (
                        SELECT
                            summoner1id, summoner2id,
                            sum(games) AS games,
                            sum(wins)  AS wins
                        FROM champion_spell_stats_agg
                        WHERE patch_version = %1$s AND platform_id = %2$s AND %3$s
                              AND champion_id = %4$d AND team_position = %5$s
                        GROUP BY summoner1id, summoner2id
                    ),
                    total AS (
                        SELECT sum(games) AS total_games FROM spell_stats
                    )
                SELECT
                    ss.summoner1id,
                    ss.summoner2id,
                    ss.games,
                    ss.wins / ss.games       AS win_rate,
                    ss.games / t.total_games AS pick_rate
                FROM spell_stats AS ss
                CROSS JOIN total AS t
                ORDER BY ss.games DESC
                LIMIT 2
                """.formatted(quote(patch), quote(platformId), tierInClause(tierFilter), championId, quote(position));

        return clickHouseJdbcTemplate.query(sql,
                (rs, rowNum) -> new ChampionSpellStatsReadModel(
                        rs.getInt("summoner1id"),
                        rs.getInt("summoner2id"),
                        rs.getLong("games"),
                        rs.getDouble("win_rate"),
                        rs.getDouble("pick_rate")));
    }

    @Override
    public List<ChampionSkillBuildReadModel> getChampionSkillBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        String sql = """
                WITH
                    skill_stats AS (
                        SELECT
                            skill_build,
                            sum(games) AS games,
                            sum(wins)  AS wins
                        FROM champion_skill_build_stats_agg
                        WHERE patch_version = %1$s AND platform_id = %2$s AND %3$s
                              AND champion_id = %4$d AND team_position = %5$s
                        GROUP BY skill_build
                    ),
                    total AS (
                        SELECT sum(games) AS total_games FROM skill_stats
                    )
                SELECT
                    sk.skill_build,
                    sk.games,
                    sk.wins / sk.games       AS win_rate,
                    sk.games / t.total_games AS pick_rate
                FROM skill_stats AS sk
                CROSS JOIN total AS t
                ORDER BY sk.games DESC
                LIMIT 2
                """.formatted(quote(patch), quote(platformId), tierInClause(tierFilter), championId, quote(position));

        return clickHouseJdbcTemplate.query(sql,
                (rs, rowNum) -> new ChampionSkillBuildReadModel(
                        rs.getString("skill_build"),
                        rs.getLong("games"),
                        rs.getDouble("win_rate"),
                        rs.getDouble("pick_rate")));
    }

    @Override
    public List<ChampionStartItemBuildReadModel> getChampionStartItemBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        String sql = """
                WITH
                    item_stats AS (
                        SELECT
                            start_items,
                            sum(games) AS games,
                            sum(wins)  AS wins
                        FROM champion_start_item_stats_agg
                        WHERE patch_version = %1$s AND platform_id = %2$s AND %3$s
                              AND champion_id = %4$d AND team_position = %5$s
                        GROUP BY start_items
                    ),
                    total AS (
                        SELECT sum(games) AS total_games FROM item_stats
                    )
                SELECT
                    its.start_items,
                    its.games,
                    its.wins / its.games       AS win_rate,
                    its.games / t.total_games  AS pick_rate
                FROM item_stats AS its
                CROSS JOIN total AS t
                ORDER BY its.games DESC
                LIMIT 2
                """.formatted(quote(patch), quote(platformId), tierInClause(tierFilter), championId, quote(position));

        return clickHouseJdbcTemplate.query(sql,
                (rs, rowNum) -> new ChampionStartItemBuildReadModel(
                        parseCsvIntArray(rs.getString("start_items")),
                        rs.getLong("games"),
                        rs.getDouble("win_rate"),
                        rs.getDouble("pick_rate")));
    }

    @Override
    public List<ChampionBootBuildReadModel> getChampionBootBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        String sql = """
                WITH
                    boot_stats AS (
                        SELECT
                            boot_id,
                            sum(games) AS games,
                            sum(wins)  AS wins
                        FROM champion_boot_stats_agg
                        WHERE patch_version = %1$s AND platform_id = %2$s AND %3$s
                              AND champion_id = %4$d AND team_position = %5$s
                        GROUP BY boot_id
                    ),
                    total AS (
                        SELECT sum(games) AS total_games FROM boot_stats
                    )
                SELECT
                    bs.boot_id,
                    bs.games,
                    bs.wins / bs.games       AS win_rate,
                    bs.games / t.total_games AS pick_rate
                FROM boot_stats AS bs
                CROSS JOIN total AS t
                ORDER BY bs.games DESC
                LIMIT 2
                """.formatted(quote(patch), quote(platformId), tierInClause(tierFilter), championId, quote(position));

        return clickHouseJdbcTemplate.query(sql,
                (rs, rowNum) -> new ChampionBootBuildReadModel(
                        rs.getInt("boot_id"),
                        rs.getLong("games"),
                        rs.getDouble("win_rate"),
                        rs.getDouble("pick_rate")));
    }

    @Override
    public List<ChampionItemBuildReadModel> getChampionItemBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        String sql = """
                WITH
                    build_stats AS (
                        SELECT item_build, sum(games) AS games, sum(wins) AS wins
                        FROM champion_item_build_stats_agg
                        WHERE patch_version = %1$s AND platform_id = %2$s AND %3$s
                              AND champion_id = %4$d AND team_position = %5$s
                        GROUP BY item_build
                    ),
                    total AS (
                        SELECT sum(games) AS total_games FROM build_stats
                    )
                SELECT
                    bs.item_build,
                    bs.games,
                    bs.wins / bs.games       AS win_rate,
                    bs.games / t.total_games AS pick_rate
                FROM build_stats AS bs
                CROSS JOIN total AS t
                ORDER BY bs.games DESC
                LIMIT 2
                """.formatted(quote(patch), quote(platformId), tierInClause(tierFilter), championId, quote(position));

        return clickHouseJdbcTemplate.query(sql,
                (rs, rowNum) -> new ChampionItemBuildReadModel(
                        parseCsvIntArray(rs.getString("item_build")),
                        rs.getLong("games"),
                        rs.getDouble("win_rate"),
                        rs.getDouble("pick_rate")));
    }

    @Override
    public Map<String, List<ChampionRateReadModel>> getChampionStatsByPosition(
            String patch, String platformId, TierFilter tierFilter) {
        String tierIn = tierInClause(tierFilter);
        String sql = """
                WITH
                    stats AS (
                        SELECT champion_id, team_position,
                               sum(games) AS games, sum(wins) AS wins
                        FROM champion_stats_agg
                        WHERE patch_version = %1$s AND platform_id = %2$s AND %3$s
                        GROUP BY champion_id, team_position
                    ),
                    pick_total AS (
                        SELECT team_position, sum(participant_rows) AS participant_rows
                        FROM match_count_agg
                        WHERE patch_version = %1$s AND platform_id = %2$s AND %3$s
                        GROUP BY team_position
                    ),
                    ban_stats AS (
                        SELECT champion_id, sum(bans) AS bans
                        FROM champion_bans_agg
                        WHERE patch_version = %1$s AND platform_id = %2$s AND %3$s
                        GROUP BY champion_id
                    ),
                    ban_total AS (
                        SELECT sum(participant_rows) AS total_participants
                        FROM match_count_agg
                        WHERE patch_version = %1$s AND platform_id = %2$s AND %3$s
                    )
                SELECT s.team_position AS team_position, s.champion_id AS champion_id,
                       coalesce(round(s.wins / nullIf(s.games, 0), 4), 0) AS win_rate,
                       coalesce(round(s.games / nullIf(pt.participant_rows, 0), 4), 0) AS pick_rate,
                       coalesce(round(coalesce(b.bans, 0) / nullIf(bt.total_participants, 0), 4), 0) AS ban_rate,
                       s.games AS total_games
                FROM stats AS s
                INNER JOIN pick_total AS pt ON s.team_position = pt.team_position
                LEFT  JOIN ban_stats  AS b  ON s.champion_id   = b.champion_id
                CROSS JOIN ban_total  AS bt
                ORDER BY s.team_position, s.games DESC
                """.formatted(quote(patch), quote(platformId), tierIn);

        return clickHouseJdbcTemplate.query(sql,
                (rs, rowNum) -> new AbstractMap.SimpleEntry<>(
                        rs.getString("team_position"),
                        new ChampionRateReadModel(
                                rs.getInt("champion_id"),
                                rs.getDouble("win_rate"),
                                rs.getDouble("pick_rate"),
                                rs.getDouble("ban_rate"),
                                rs.getLong("total_games"))))
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }
}
