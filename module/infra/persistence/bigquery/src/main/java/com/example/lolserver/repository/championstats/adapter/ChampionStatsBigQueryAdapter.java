package com.example.lolserver.repository.championstats.adapter;

import com.example.lolserver.Tier;
import com.example.lolserver.TierFilter;
import com.example.lolserver.config.BigQueryProperties;
import com.example.lolserver.domain.championstats.application.model.ChampionBootBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionItemStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionMatchupReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRateReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRuneBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSkillBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSpellStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionStartItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionWinRateReadModel;
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsQueryPort;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "stats.datasource", havingValue = "bigquery")
public class ChampionStatsBigQueryAdapter implements ChampionStatsQueryPort {

    private final BigQuery bigQuery;
    private final String dataset;

    public ChampionStatsBigQueryAdapter(BigQuery bigQuery, BigQueryProperties properties) {
        this.bigQuery = bigQuery;
        this.dataset = properties.dataset();
    }

    @Override
    public List<ChampionWinRateReadModel> getChampionWinRates(
            int championId, String patch, String platformId, TierFilter tierFilter) {
        String sql = ChampionStatsBigQuerySqls.WIN_RATES.formatted(table("mv_champion_pick_stats"));

        QueryJobConfiguration job = baseQuery(sql, patch, platformId, tierFilter)
                .addNamedParameter("championId", QueryParameterValue.int64(championId))
                .build();

        return query(job, row -> new ChampionWinRateReadModel(
                row.get("team_position").getStringValue(),
                row.get("total_games").getLongValue(),
                row.get("total_wins").getLongValue(),
                row.get("total_win_rate").getDoubleValue()
        ));
    }

    @Override
    public List<ChampionMatchupReadModel> getStrongMatchups(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        return queryMatchups(championId, patch, platformId, tierFilter, position, "DESC");
    }

    @Override
    public List<ChampionMatchupReadModel> getWeakMatchups(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        return queryMatchups(championId, patch, platformId, tierFilter, position, "ASC");
    }

    private List<ChampionMatchupReadModel> queryMatchups(
            int championId, String patch, String platformId,
            TierFilter tierFilter, String position, String orderDirection) {
        String sql = ChampionStatsBigQuerySqls.MATCHUPS
                .formatted(table("mv_champion_matchup_stats"), orderDirection);

        QueryJobConfiguration job = championPositionQuery(sql, patch, platformId, tierFilter, championId, position)
                .build();

        return query(job, row -> new ChampionMatchupReadModel(
                getInt(row, "opponent_champion_id"),
                row.get("games").getLongValue(),
                row.get("win_rate").getDoubleValue(),
                row.get("pick_rate").getDoubleValue()
        ));
    }

    @Override
    public List<ChampionRuneBuildReadModel> getChampionRuneBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        String sql = ChampionStatsBigQuerySqls.RUNE_BUILDS.formatted(table("mv_champion_rune_stats"));

        QueryJobConfiguration job = championPositionQuery(sql, patch, platformId, tierFilter, championId, position)
                .build();

        return query(job, row -> new ChampionRuneBuildReadModel(
                getInt(row, "primary_style_id"),
                getInt(row, "sub_style_id"),
                getInt(row, "primary_perk0"),
                getInt(row, "primary_perk1"),
                getInt(row, "primary_perk2"),
                getInt(row, "primary_perk3"),
                getInt(row, "sub_perk0"),
                getInt(row, "sub_perk1"),
                row.get("games").getLongValue(),
                row.get("win_rate").getDoubleValue(),
                row.get("pick_rate").getDoubleValue()
        ));
    }

    @Override
    public List<ChampionSpellStatsReadModel> getChampionSpellStats(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        String sql = ChampionStatsBigQuerySqls.SPELL_STATS.formatted(table("mv_champion_spell_stats"));

        QueryJobConfiguration job = championPositionQuery(sql, patch, platformId, tierFilter, championId, position)
                .build();

        return query(job, row -> new ChampionSpellStatsReadModel(
                getInt(row, "summoner1id"),
                getInt(row, "summoner2id"),
                row.get("games").getLongValue(),
                row.get("win_rate").getDoubleValue(),
                row.get("pick_rate").getDoubleValue()
        ));
    }

    @Override
    public List<ChampionSkillBuildReadModel> getChampionSkillBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        String sql = ChampionStatsBigQuerySqls.SKILL_BUILDS.formatted(table("mv_champion_skill_build_stats"));

        QueryJobConfiguration job = championPositionQuery(sql, patch, platformId, tierFilter, championId, position)
                .build();

        return query(job, row -> new ChampionSkillBuildReadModel(
                row.get("skill_build").getStringValue(),
                row.get("games").getLongValue(),
                row.get("win_rate").getDoubleValue(),
                row.get("pick_rate").getDoubleValue()
        ));
    }

    @Override
    public List<ChampionStartItemBuildReadModel> getChampionStartItemBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        String sql = ChampionStatsBigQuerySqls.START_ITEM_BUILDS.formatted(table("mv_champion_start_item_stats"));

        QueryJobConfiguration job = championPositionQuery(sql, patch, platformId, tierFilter, championId, position)
                .build();

        return query(job, row -> new ChampionStartItemBuildReadModel(
                row.get("start_items").getStringValue(),
                row.get("games").getLongValue(),
                row.get("win_rate").getDoubleValue(),
                row.get("pick_rate").getDoubleValue()
        ));
    }

    @Override
    public List<ChampionBootBuildReadModel> getChampionBootBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        String sql = ChampionStatsBigQuerySqls.BOOT_BUILDS.formatted(table("mv_champion_boot_stats"));

        QueryJobConfiguration job = championPositionQuery(sql, patch, platformId, tierFilter, championId, position)
                .build();

        return query(job, row -> new ChampionBootBuildReadModel(
                getInt(row, "boot_id"),
                row.get("games").getLongValue(),
                row.get("win_rate").getDoubleValue(),
                row.get("pick_rate").getDoubleValue()
        ));
    }

    @Override
    public List<ChampionItemBuildReadModel> getChampionItemBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position) {
        String sql = ChampionStatsBigQuerySqls.ITEM_BUILDS.formatted(table("mv_champion_item_build_stats"));

        QueryJobConfiguration job = championPositionQuery(sql, patch, platformId, tierFilter, championId, position)
                .build();

        return query(job, row -> new ChampionItemBuildReadModel(
                row.get("item_build").getStringValue(),
                row.get("games").getLongValue(),
                row.get("win_rate").getDoubleValue(),
                row.get("pick_rate").getDoubleValue()
        ));
    }

    @Override
    public List<ChampionItemStatsReadModel> getChampionItemStats(
            int championId, String patch, String platformId, TierFilter tierFilter, String position, int itemOrder) {
        String sql = ChampionStatsBigQuerySqls.ITEM_STATS.formatted(
                table("mv_champion_item_stats"),
                table("mv_champion_pick_stats"),
                table("legendary_items"));

        QueryJobConfiguration job = championPositionQuery(sql, patch, platformId, tierFilter, championId, position)
                .addNamedParameter("itemOrder", QueryParameterValue.int64(itemOrder))
                .build();

        return query(job, row -> new ChampionItemStatsReadModel(
                getInt(row, "item_id"),
                row.get("item_name").getStringValue(),
                row.get("games").getLongValue(),
                row.get("win_rate").getDoubleValue(),
                row.get("pick_rate").getDoubleValue()
        ));
    }

    @Override
    public Map<String, List<ChampionRateReadModel>> getChampionStatsByPosition(
            String patch, String platformId, TierFilter tierFilter) {
        String sql = ChampionStatsBigQuerySqls.STATS_BY_POSITION.formatted(
                table("mv_match_count_stats"),
                table("mv_champion_ban_stats"),
                table("mv_champion_pick_stats"));

        QueryJobConfiguration job = baseQuery(sql, patch, platformId, tierFilter).build();

        List<AbstractMap.SimpleEntry<String, ChampionRateReadModel>> rows = query(job, row ->
                new AbstractMap.SimpleEntry<>(
                        row.get("team_position").getStringValue(),
                        new ChampionRateReadModel(
                                getInt(row, "champion_id"),
                                row.get("win_rate").getDoubleValue(),
                                row.get("pick_rate").getDoubleValue(),
                                row.get("ban_rate").getDoubleValue(),
                                row.get("total_games").getLongValue()
                        )
                ));

        return rows.stream().collect(Collectors.groupingBy(
                Map.Entry::getKey,
                Collectors.mapping(Map.Entry::getValue, Collectors.toList())
        ));
    }

    private String table(String name) {
        return "`" + dataset + "." + name + "`";
    }

    private QueryJobConfiguration.Builder baseQuery(
            String sql, String patch, String platformId, TierFilter tierFilter) {
        return QueryJobConfiguration.newBuilder(sql)
                .setUseLegacySql(false)
                .addNamedParameter("patch", QueryParameterValue.int64(toPatchVersionInt(patch)))
                .addNamedParameter("platform", QueryParameterValue.string(platformId))
                .addNamedParameter("tierBuckets", QueryParameterValue.array(
                        toTierBuckets(tierFilter), StandardSQLTypeName.INT64));
    }

    private QueryJobConfiguration.Builder championPositionQuery(
            String sql, String patch, String platformId, TierFilter tierFilter,
            int championId, String position) {
        return baseQuery(sql, patch, platformId, tierFilter)
                .addNamedParameter("championId", QueryParameterValue.int64(championId))
                .addNamedParameter("position", QueryParameterValue.string(position));
    }

    private static long toPatchVersionInt(String patch) {
        String[] parts = patch.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Unsupported patch format: " + patch);
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            return (long) major * 100 + minor;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unsupported patch format: " + patch, e);
        }
    }

    private static Long[] toTierBuckets(TierFilter tierFilter) {
        return tierFilter.getTierNames().stream()
                .map(name -> (long) Tier.valueOf(name).getScore())
                .toArray(Long[]::new);
    }

    private static int getInt(FieldValueList row, String column) {
        return (int) row.get(column).getLongValue();
    }

    private <T> List<T> query(QueryJobConfiguration job, Function<FieldValueList, T> mapper) {
        TableResult result;
        try {
            result = bigQuery.query(job);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("BigQuery query interrupted", e);
        }
        List<T> rows = new ArrayList<>();
        for (FieldValueList row : result.iterateAll()) {
            rows.add(mapper.apply(row));
        }
        return rows;
    }
}
