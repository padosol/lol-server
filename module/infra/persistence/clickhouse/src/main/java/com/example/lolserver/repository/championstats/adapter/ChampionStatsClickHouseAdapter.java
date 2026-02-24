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

import java.util.List;

@Slf4j
@Component
public class ChampionStatsClickHouseAdapter implements ChampionStatsQueryPort {

    private final JdbcTemplate clickHouseJdbcTemplate;

    public ChampionStatsClickHouseAdapter(
            @Qualifier("clickHouseJdbcTemplate") JdbcTemplate clickHouseJdbcTemplate) {
        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
    }

    @Override
    public List<ChampionWinRateResponse> getChampionWinRates(
            int championId, String patch, String platformId) {
        String sql = """
            SELECT champion_id, team_position,
                   sum(games) AS total_games,
                   sum(wins) AS total_wins,
                   round(sum(wins) / sum(games), 4) AS total_win_rate
            FROM championstats_local
            WHERE champion_id = ? AND patch = ? AND platform_id = ?
            GROUP BY champion_id, team_position, platform_id
            ORDER BY total_games DESC
            """;

        return clickHouseJdbcTemplate.query(sql,
            (rs, rowNum) -> new ChampionWinRateResponse(
                rs.getInt("champion_id"),
                rs.getString("team_position"),
                rs.getLong("total_games"),
                rs.getLong("total_wins"),
                rs.getDouble("total_win_rate")
            ),
            championId, patch, platformId);
    }

    @Override
    public List<ChampionMatchupResponse> getChampionMatchups(
            int championId, String patch, String platformId) {
        String sql = """
            SELECT champion_id, opponent_champion_id, team_position,
                   sum(games) AS total_games,
                   sum(wins) AS total_wins,
                   round(sum(wins) / sum(games), 4) AS total_win_rate
            FROM champion_matchup_stats_local
            WHERE champion_id = ? AND patch = ? AND platform_id = ?
            GROUP BY champion_id, opponent_champion_id, team_position
            ORDER BY total_win_rate DESC, total_games DESC
            """;

        return clickHouseJdbcTemplate.query(sql,
            (rs, rowNum) -> new ChampionMatchupResponse(
                rs.getInt("champion_id"),
                rs.getInt("opponent_champion_id"),
                rs.getString("team_position"),
                rs.getLong("total_games"),
                rs.getLong("total_wins"),
                rs.getDouble("total_win_rate")
            ),
            championId, patch, platformId);
    }

    @Override
    public List<ChampionItemBuildResponse> getChampionItemBuilds(
            int championId, String patch, String platformId) {
        String sql = """
            SELECT champion_id, team_position, items_sorted,
                   sum(games) AS total_games,
                   sum(wins) AS total_wins,
                   round(sum(wins) / sum(games), 4) AS total_win_rate
            FROM item_build_stats_local
            WHERE champion_id = ? AND patch = ? AND platform_id = ?
            GROUP BY champion_id, team_position, items_sorted
            ORDER BY total_games DESC
            LIMIT 5
            """;

        return clickHouseJdbcTemplate.query(sql,
            (rs, rowNum) -> new ChampionItemBuildResponse(
                rs.getInt("champion_id"),
                rs.getString("team_position"),
                rs.getString("items_sorted"),
                rs.getLong("total_games"),
                rs.getLong("total_wins"),
                rs.getDouble("total_win_rate")
            ),
            championId, patch, platformId);
    }

    @Override
    public List<ChampionRuneBuildResponse> getChampionRuneBuilds(
            int championId, String patch, String platformId) {
        String sql = """
            SELECT champion_id, team_position,
                   primary_style_id, primary_perk_ids,
                   sub_style_id, sub_perk_ids,
                   sum(games) AS total_games,
                   sum(wins) AS total_wins,
                   round(sum(wins) / sum(games), 4) AS total_win_rate
            FROM rune_build_stats_local
            WHERE champion_id = ? AND patch = ? AND platform_id = ?
            GROUP BY champion_id, team_position,
                     primary_style_id, primary_perk_ids, sub_style_id, sub_perk_ids
            ORDER BY total_games DESC, total_win_rate DESC
            LIMIT 5
            """;

        return clickHouseJdbcTemplate.query(sql,
            (rs, rowNum) -> new ChampionRuneBuildResponse(
                rs.getInt("champion_id"),
                rs.getString("team_position"),
                rs.getInt("primary_style_id"),
                rs.getString("primary_perk_ids"),
                rs.getInt("sub_style_id"),
                rs.getString("sub_perk_ids"),
                rs.getLong("total_games"),
                rs.getLong("total_wins"),
                rs.getDouble("total_win_rate")
            ),
            championId, patch, platformId);
    }

    @Override
    public List<ChampionSkillBuildResponse> getChampionSkillBuilds(
            int championId, String patch, String platformId) {
        String sql = """
            SELECT champion_id, team_position, skill_order_15,
                   sum(games) AS total_games,
                   sum(wins) AS total_wins,
                   round(sum(wins) / sum(games), 4) AS total_win_rate
            FROM skill_build_stats_local
            WHERE champion_id = ? AND patch = ? AND platform_id = ?
            GROUP BY champion_id, team_position, skill_order_15
            ORDER BY total_games DESC, total_win_rate DESC
            LIMIT 5
            """;

        return clickHouseJdbcTemplate.query(sql,
            (rs, rowNum) -> new ChampionSkillBuildResponse(
                rs.getInt("champion_id"),
                rs.getString("team_position"),
                rs.getString("skill_order_15"),
                rs.getLong("total_games"),
                rs.getLong("total_wins"),
                rs.getDouble("total_win_rate")
            ),
            championId, patch, platformId);
    }
}
