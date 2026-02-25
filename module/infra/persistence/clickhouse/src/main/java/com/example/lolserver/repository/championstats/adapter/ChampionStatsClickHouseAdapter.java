package com.example.lolserver.repository.championstats.adapter;

import com.example.lolserver.domain.championstats.application.dto.ChampionItemBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionMatchupResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionRuneBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionSkillBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionWinRateResponse;
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsQueryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
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

    @Override
    public List<ChampionWinRateResponse> getChampionWinRates(
            int championId, String patch, String platformId, String tier) {
        String sql = """
                SELECT team_position,
                       toInt64(sum(games)) AS total_games,
                       toInt64(sum(wins)) AS total_wins,
                       coalesce(round(sum(wins) / nullIf(sum(games), 0), 4), 0) AS total_win_rate
                FROM champion_stats_local
                WHERE champion_id = %d AND patch = %s AND platform_id = %s AND tier = %s
                GROUP BY team_position
                ORDER BY total_games DESC
                """.formatted(championId, quote(patch), quote(platformId), quote(tier));

        return clickHouseJdbcTemplate.query(sql,
                (rs, rowNum) -> new ChampionWinRateResponse(
                        rs.getString("team_position"),
                        rs.getLong("total_games"),
                        rs.getLong("total_wins"),
                        rs.getDouble("total_win_rate")
                ));
    }

    @Override
    public Map<String, List<ChampionMatchupResponse>> getChampionMatchups(
            int championId, String patch, String platformId, String tier) {
        String sql = """
                SELECT team_position, opponent_champion_id,
                       sum(games) AS total_games,
                       sum(wins) AS total_wins,
                       round(sum(wins) / nullIf(sum(games), 0), 4) AS total_win_rate
                FROM champion_matchup_stats_local
                WHERE champion_id = %d AND patch = %s AND platform_id = %s AND tier = %s
                GROUP BY team_position, opponent_champion_id
                ORDER BY total_win_rate DESC, total_games DESC
                """.formatted(championId, quote(patch), quote(platformId), quote(tier));

        return clickHouseJdbcTemplate.query(sql,
                (rs, rowNum) -> new AbstractMap.SimpleEntry<>(
                        rs.getString("team_position"),
                        new ChampionMatchupResponse(
                                rs.getInt("opponent_champion_id"),
                                rs.getLong("total_games"),
                                rs.getLong("total_wins"),
                                rs.getDouble("total_win_rate")
                        )
                )).stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }

    @Override
    public Map<String, List<ChampionItemBuildResponse>> getChampionItemBuilds(
            int championId, String patch, String platformId, String tier) {
        String sql = """
                SELECT team_position, items_sorted, total_games, total_wins, total_win_rate
                FROM (
                    SELECT team_position, items_sorted,
                           sum(games) AS total_games,
                           sum(wins) AS total_wins,
                           round(sum(wins) / nullIf(sum(games), 0), 4) AS total_win_rate,
                           ROW_NUMBER() OVER (PARTITION BY team_position ORDER BY sum(games) DESC) AS rn
                    FROM item_build_stats_local
                    WHERE champion_id = %d AND patch = %s AND platform_id = %s AND tier = %s
                    GROUP BY team_position, items_sorted
                ) sub
                WHERE rn <= 5
                ORDER BY team_position, total_games DESC
                """.formatted(championId, quote(patch), quote(platformId), quote(tier));

        return clickHouseJdbcTemplate.query(sql,
                (rs, rowNum) -> new AbstractMap.SimpleEntry<>(
                        rs.getString("team_position"),
                        new ChampionItemBuildResponse(
                                rs.getString("items_sorted"),
                                rs.getLong("total_games"),
                                rs.getLong("total_wins"),
                                rs.getDouble("total_win_rate")
                        )
                )).stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }

    @Override
    public Map<String, List<ChampionRuneBuildResponse>> getChampionRuneBuilds(
            int championId, String patch, String platformId, String tier) {
        String sql = """
                SELECT team_position, primary_style_id, primary_perk_ids,
                       sub_style_id, sub_perk_ids, total_games, total_wins, total_win_rate
                FROM (
                    SELECT team_position,
                           primary_style_id, primary_perk_ids,
                           sub_style_id, sub_perk_ids,
                           sum(games) AS total_games,
                           sum(wins) AS total_wins,
                           round(sum(wins) / nullIf(sum(games), 0), 4) AS total_win_rate,
                           ROW_NUMBER() OVER (PARTITION BY team_position ORDER BY sum(games) DESC) AS rn
                    FROM rune_build_stats_local
                    WHERE champion_id = %d AND patch = %s AND platform_id = %s AND tier = %s
                    GROUP BY team_position,
                             primary_style_id, primary_perk_ids, sub_style_id, sub_perk_ids
                ) sub
                WHERE rn <= 5
                ORDER BY team_position, total_games DESC
                """.formatted(championId, quote(patch), quote(platformId), quote(tier));

        return clickHouseJdbcTemplate.query(sql,
                (rs, rowNum) -> new AbstractMap.SimpleEntry<>(
                        rs.getString("team_position"),
                        new ChampionRuneBuildResponse(
                                rs.getInt("primary_style_id"),
                                rs.getString("primary_perk_ids"),
                                rs.getInt("sub_style_id"),
                                rs.getString("sub_perk_ids"),
                                rs.getLong("total_games"),
                                rs.getLong("total_wins"),
                                rs.getDouble("total_win_rate")
                        )
                )).stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }

    @Override
    public Map<String, List<ChampionSkillBuildResponse>> getChampionSkillBuilds(
            int championId, String patch, String platformId, String tier) {
        String sql = """
                SELECT team_position, skill_order_15, total_games, total_wins, total_win_rate
                FROM (
                    SELECT team_position, skill_order_15,
                           sum(games) AS total_games,
                           sum(wins) AS total_wins,
                           round(sum(wins) / nullIf(sum(games), 0), 4) AS total_win_rate,
                           ROW_NUMBER() OVER (PARTITION BY team_position ORDER BY sum(games) DESC) AS rn
                    FROM skill_build_stats_local
                    WHERE champion_id = %d AND patch = %s AND platform_id = %s AND tier = %s
                    GROUP BY team_position, skill_order_15
                ) sub
                WHERE rn <= 5
                ORDER BY team_position, total_games DESC
                """.formatted(championId, quote(patch), quote(platformId), quote(tier));

        return clickHouseJdbcTemplate.query(sql,
                (rs, rowNum) -> new AbstractMap.SimpleEntry<>(
                        rs.getString("team_position"),
                        new ChampionSkillBuildResponse(
                                rs.getString("skill_order_15"),
                                rs.getLong("total_games"),
                                rs.getLong("total_wins"),
                                rs.getDouble("total_win_rate")
                        )
                )).stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }
}
