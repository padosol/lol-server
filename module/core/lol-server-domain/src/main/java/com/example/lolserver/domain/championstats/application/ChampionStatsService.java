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
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChampionStatsService {

    private final ChampionStatsQueryPort championStatsQueryPort;

    public ChampionStatsReadModel getChampionStats(
            int championId, String patch, String platformId, TierFilter tierFilter) {

        List<ChampionWinRateReadModel> winRates =
            championStatsQueryPort.getChampionWinRates(championId, patch, platformId, tierFilter);

        List<ChampionPositionStatsReadModel> positions = winRates.stream()
            .map(wr -> buildPositionStats(championId, patch, platformId, tierFilter, wr))
            .toList();

        return new ChampionStatsReadModel(tierFilter.toDisplayString(), positions);
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
        Map<String, List<ChampionRateReadModel>> groupedByPosition =
                championStatsQueryPort.getChampionStatsByPosition(patch, platformId, tierFilter);

        return groupedByPosition.entrySet().stream()
                .map(entry -> new PositionChampionStatsReadModel(
                        entry.getKey(),
                        ChampionTierCalculator.assignTiers(entry.getValue())
                ))
                .toList();
    }
}
