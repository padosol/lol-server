package com.example.lolserver.repository.championstats.adapter;

import com.example.lolserver.Tier;
import com.example.lolserver.TierFilter;
import com.example.lolserver.domain.championstats.application.model.TimelineFrameReadModel;
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsTimelineQueryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChampionStatsTimelinePostgresAdapter implements ChampionStatsTimelineQueryPort {

    private static final List<Long> FRAME_TIMESTAMPS_MS =
            List.of(600_000L, 900_000L, 1_200_000L, 1_500_000L, 1_800_000L);

    private static final String SQL = """
            SELECT mp.team_position                                          AS team_position,
                   (pf.timestamp / 60000)                                    AS minute,
                   AVG(pf.total_gold)                                        AS avg_gold,
                   AVG(pf.minions_killed + pf.jungle_minions_killed)         AS avg_cs,
                   AVG(pf.xp)                                                AS avg_xp,
                   COUNT(*)                                                  AS sample_size
            FROM match_participant mp
                 JOIN match m              ON m.match_id  = mp.match_id
                 JOIN participant_frame pf ON pf.match_id = mp.match_id
                                          AND pf.participant_id = mp.participant_id
            WHERE mp.champion_id = :championId
              AND m.patch_version = :patch
              AND m.platform_id   = :platformId
              AND m.average_tier  IN (:tierScores)
              AND pf.timestamp    IN (:frameTimestamps)
              AND mp.team_position IS NOT NULL
              AND mp.team_position <> ''
            GROUP BY mp.team_position, pf.timestamp
            ORDER BY mp.team_position, pf.timestamp
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ChampionStatsTimelinePostgresAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, List<TimelineFrameReadModel>> aggregateChampionTimeline(
            int championId, String patch, String platformId, TierFilter tierFilter) {
        log.info("timeline 집계 — championId: {}, patch: {}, platformId: {}, tier: {}",
                championId, patch, platformId, tierFilter);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("championId", championId)
                .addValue("patch", patch)
                .addValue("platformId", platformId)
                .addValue("tierScores", toTierScores(tierFilter))
                .addValue("frameTimestamps", FRAME_TIMESTAMPS_MS);

        return jdbcTemplate.query(SQL, params, (rs, rowNum) ->
                        new AbstractMap.SimpleEntry<>(
                                rs.getString("team_position"),
                                new TimelineFrameReadModel(
                                        rs.getInt("minute"),
                                        rs.getDouble("avg_gold"),
                                        rs.getDouble("avg_cs"),
                                        rs.getDouble("avg_xp"),
                                        rs.getLong("sample_size"))))
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private static List<Integer> toTierScores(TierFilter tierFilter) {
        return tierFilter.getTierNames().stream()
                .map(name -> Tier.valueOf(name).getScore())
                .toList();
    }
}
