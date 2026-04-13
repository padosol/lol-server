package com.example.lolserver.domain.championstats.application;

import com.example.lolserver.TierFilter;
import com.example.lolserver.domain.championstats.application.model.ChampionItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionItemStatsReadModel;
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
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsCachePort;
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsQueryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ChampionStatsService {

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

        List<ChampionPositionStatsReadModel> positions = winRates.stream()
            .map(wr -> buildPositionStats(championId, patch, platformId, tierFilter, wr))
            .toList();

        ChampionStatsReadModel result = new ChampionStatsReadModel(tierDisplay, positions);

        if (cacheEnabled) {
            championStatsCachePort.saveChampionStats(championId, patch, platformId, tierDisplay, result);
        }

        return result;
    }

    private ChampionPositionStatsReadModel buildPositionStats(
            int championId, String patch, String platformId, TierFilter tierFilter,
            ChampionWinRateReadModel winRate) {
        String position = winRate.teamPosition();

        List<ChampionRuneBuildReadModel> runeBuilds =
            championStatsQueryPort.getChampionRuneBuilds(championId, patch, platformId, tierFilter, position);
        List<ChampionSpellStatsReadModel> spellStats =
            championStatsQueryPort.getChampionSpellStats(championId, patch, platformId, tierFilter, position);
        List<ChampionSkillBuildReadModel> skillBuilds =
            championStatsQueryPort.getChampionSkillBuilds(championId, patch, platformId, tierFilter, position);
        List<ChampionStartItemBuildReadModel> startItemBuilds =
            championStatsQueryPort.getChampionStartItemBuilds(championId, patch, platformId, tierFilter, position);
        List<ChampionItemBuildReadModel> itemBuilds =
            championStatsQueryPort.getChampionItemBuilds(championId, patch, platformId, tierFilter, position);

        List<ChampionMatchupReadModel> strongMatchups =
            championStatsQueryPort.getStrongMatchups(championId, patch, platformId, tierFilter, position);
        List<ChampionMatchupReadModel> weakMatchups =
            championStatsQueryPort.getWeakMatchups(championId, patch, platformId, tierFilter, position);

        Map<Integer, List<ChampionItemStatsReadModel>> itemStatsByOrder = new LinkedHashMap<>();
        for (int order = 1; order <= 3; order++) {
            itemStatsByOrder.put(order,
                championStatsQueryPort.getChampionItemStats(
                    championId, patch, platformId, tierFilter, position, order));
        }

        return new ChampionPositionStatsReadModel(
            position,
            winRate.totalWinRate(),
            winRate.totalGames(),
            strongMatchups, weakMatchups, runeBuilds, spellStats, skillBuilds,
            startItemBuilds, itemBuilds, itemStatsByOrder
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
                        ChampionTierCalculator.assignTiers(entry.getValue())
                ))
                .toList();

        if (cacheEnabled) {
            championStatsCachePort.saveChampionStatsByPosition(patch, platformId, tierDisplay, result);
        }

        return result;
    }
}
