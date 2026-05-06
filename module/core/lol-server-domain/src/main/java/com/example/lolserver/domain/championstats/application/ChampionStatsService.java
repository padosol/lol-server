package com.example.lolserver.domain.championstats.application;

import com.example.lolserver.TierFilter;
import com.example.lolserver.domain.championstats.application.model.ChampionAverageStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionBootBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionMatchupReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionPositionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRuneBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSkillBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSpellStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionStartItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRateReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionWinRateReadModel;
import com.example.lolserver.domain.championstats.application.model.PositionChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.port.in.ChampionStatsQueryUseCase;
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsCachePort;
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsQueryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ChampionStatsService implements ChampionStatsQueryUseCase {

    private final ChampionStatsQueryPort championStatsQueryPort;
    private final ChampionStatsCachePort championStatsCachePort;
    private final boolean cacheEnabled;

    public ChampionStatsService(
            ChampionStatsQueryPort championStatsQueryPort,
            ChampionStatsCachePort championStatsCachePort,
            @Value("${champion-stats.cache.enabled:true}") boolean cacheEnabled) {
        this.championStatsQueryPort = championStatsQueryPort;
        this.championStatsCachePort = championStatsCachePort;
        this.cacheEnabled = cacheEnabled;
    }

    public ChampionStatsReadModel getChampionStats(
            int championId, String patch, String platformId, TierFilter tierFilter) {

        String tierDisplay = tierFilter.toDisplayString();

        if (cacheEnabled) {
            ChampionStatsReadModel cached = championStatsCachePort
                    .findChampionStats(championId, patch, platformId, tierDisplay);
            if (cached != null) {
                log.debug("캐시 히트 - championId: {}, patch: {}, tier: {}", championId, patch, tierDisplay);
                return cached;
            }
        }

        List<ChampionWinRateReadModel> winRates =
            championStatsQueryPort.getChampionWinRates(championId, patch, platformId, tierFilter);

        // pickRate/banRate/tier 는 by-position 결과와 일관성 유지를 위해 같은 데이터를 lookup.
        // by-position 쪽은 별도 캐시 키 — hit 시 무비용, miss 면 한 번 BQ 호출.
        Map<String, ChampionRateReadModel> rateByPosition =
                lookupChampionRateByPosition(championId, patch, platformId, tierFilter);

        Map<String, ChampionAverageStatsReadModel> averagesByPosition =
                championStatsQueryPort.getChampionAverageStats(championId, patch, platformId, tierFilter)
                        .stream()
                        .collect(Collectors.toMap(
                                ChampionAverageStatsReadModel::teamPosition, a -> a));

        List<ChampionPositionStatsReadModel> positions = winRates.stream()
            .map(wr -> buildPositionStats(championId, patch, platformId, tierFilter, wr,
                    rateByPosition.get(wr.teamPosition()),
                    averagesByPosition.get(wr.teamPosition())))
            .toList();

        ChampionStatsReadModel result = new ChampionStatsReadModel(tierDisplay, positions);

        if (cacheEnabled) {
            championStatsCachePort.saveChampionStats(championId, patch, platformId, tierDisplay, result);
        }

        return result;
    }

    private Map<String, ChampionRateReadModel> lookupChampionRateByPosition(
            int championId, String patch, String platformId, TierFilter tierFilter) {
        List<PositionChampionStatsReadModel> byPosition =
                getChampionStatsByPosition(patch, platformId, tierFilter);

        Map<String, ChampionRateReadModel> result = new HashMap<>();
        for (PositionChampionStatsReadModel position : byPosition) {
            for (ChampionRateReadModel rate : position.champions()) {
                if (rate.championId() == championId) {
                    result.put(position.teamPosition(), rate);
                    break;
                }
            }
        }
        return result;
    }

    private ChampionPositionStatsReadModel buildPositionStats(
            int championId, String patch, String platformId, TierFilter tierFilter,
            ChampionWinRateReadModel winRate, ChampionRateReadModel rate,
            ChampionAverageStatsReadModel averages) {
        String position = winRate.teamPosition();

        List<ChampionMatchupReadModel> matchups =
            championStatsQueryPort.getChampionMatchups(championId, patch, platformId, tierFilter, position);
        List<ChampionRuneBuildReadModel> runeBuilds =
            championStatsQueryPort.getChampionRuneBuilds(championId, patch, platformId, tierFilter, position);
        List<ChampionSpellStatsReadModel> spellStats =
            championStatsQueryPort.getChampionSpellStats(championId, patch, platformId, tierFilter, position);
        List<ChampionSkillBuildReadModel> skillBuilds =
            championStatsQueryPort.getChampionSkillBuilds(championId, patch, platformId, tierFilter, position);
        List<ChampionStartItemBuildReadModel> startItemBuilds =
            championStatsQueryPort.getChampionStartItemBuilds(championId, patch, platformId, tierFilter, position);
        List<ChampionBootBuildReadModel> bootBuilds =
            championStatsQueryPort.getChampionBootBuilds(championId, patch, platformId, tierFilter, position);
        List<ChampionItemBuildReadModel> itemBuilds =
            championStatsQueryPort.getChampionItemBuilds(championId, patch, platformId, tierFilter, position);

        double pickRate = rate != null ? rate.pickRate() : 0.0;
        double banRate = rate != null ? rate.banRate() : 0.0;
        String tier = rate != null ? rate.tier() : null;

        return new ChampionPositionStatsReadModel(
            position,
            winRate.totalWinRate(),
            pickRate,
            banRate,
            tier,
            winRate.totalGames(),
            averages,
            matchups, runeBuilds, spellStats, skillBuilds,
            startItemBuilds, bootBuilds, itemBuilds
        );
    }

    public List<PositionChampionStatsReadModel> getChampionStatsByPosition(
            String patch, String platformId, TierFilter tierFilter) {

        String tierDisplay = tierFilter.toDisplayString();

        if (cacheEnabled) {
            List<PositionChampionStatsReadModel> cached = championStatsCachePort
                    .findChampionStatsByPosition(patch, platformId, tierDisplay);
            if (cached != null) {
                log.debug("캐시 히트 - positions, patch: {}, tier: {}", patch, tierDisplay);
                return cached;
            }
        }

        Map<String, List<ChampionRateReadModel>> groupedByPosition =
                championStatsQueryPort.getChampionStatsByPosition(patch, platformId, tierFilter);

        List<PositionChampionStatsReadModel> result = groupedByPosition.entrySet().stream()
                .map(entry -> new PositionChampionStatsReadModel(
                        entry.getKey(),
                        assignTiersIfMissing(entry.getValue())
                ))
                .toList();

        if (cacheEnabled) {
            championStatsCachePort.saveChampionStatsByPosition(patch, platformId, tierDisplay, result);
        }

        return result;
    }

    private static List<ChampionRateReadModel> assignTiersIfMissing(List<ChampionRateReadModel> champions) {
        if (!champions.isEmpty() && champions.get(0).tier() != null) {
            return champions;
        }
        return ChampionTierCalculator.assignTiers(champions);
    }
}
