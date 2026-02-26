package com.example.lolserver.domain.championstats.application;

import com.example.lolserver.domain.championstats.application.model.ChampionItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionMatchupReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionPositionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRuneBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSkillBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionWinRateReadModel;
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChampionStatsService {

    private final ChampionStatsQueryPort championStatsQueryPort;

    public ChampionStatsReadModel getChampionStats(int championId, String patch, String platformId, String tier) {
        List<ChampionWinRateReadModel> winRates =
            championStatsQueryPort.getChampionWinRates(championId, patch, platformId, tier);
        Map<String, List<ChampionMatchupReadModel>> matchupsByPos =
            championStatsQueryPort.getChampionMatchups(championId, patch, platformId, tier);
        Map<String, List<ChampionItemBuildReadModel>> itemBuildsByPos =
            championStatsQueryPort.getChampionItemBuilds(championId, patch, platformId, tier);
        Map<String, List<ChampionRuneBuildReadModel>> runeBuildsByPos =
            championStatsQueryPort.getChampionRuneBuilds(championId, patch, platformId, tier);
        Map<String, List<ChampionSkillBuildReadModel>> skillBuildsByPos =
            championStatsQueryPort.getChampionSkillBuilds(championId, patch, platformId, tier);

        List<ChampionPositionStatsReadModel> stats = winRates.stream()
            .map(wr -> new ChampionPositionStatsReadModel(
                wr.teamPosition(),
                wr.totalWinRate(),
                wr.totalGames(),
                matchupsByPos.getOrDefault(wr.teamPosition(), List.of()),
                itemBuildsByPos.getOrDefault(wr.teamPosition(), List.of()),
                runeBuildsByPos.getOrDefault(wr.teamPosition(), List.of()),
                skillBuildsByPos.getOrDefault(wr.teamPosition(), List.of())
            ))
            .toList();

        return new ChampionStatsReadModel(tier, stats);
    }
}
