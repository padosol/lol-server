package com.example.lolserver.domain.championstats.application;

import com.example.lolserver.domain.championstats.application.dto.ChampionItemBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionMatchupResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionPositionStatsResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionRuneBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionSkillBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionStatsResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionWinRateResponse;
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

    public ChampionStatsResponse getChampionStats(int championId, String patch, String platformId, String tier) {
        List<ChampionWinRateResponse> winRates =
            championStatsQueryPort.getChampionWinRates(championId, patch, platformId, tier);
        Map<String, List<ChampionMatchupResponse>> matchupsByPos =
            championStatsQueryPort.getChampionMatchups(championId, patch, platformId, tier);
        Map<String, List<ChampionItemBuildResponse>> itemBuildsByPos =
            championStatsQueryPort.getChampionItemBuilds(championId, patch, platformId, tier);
        Map<String, List<ChampionRuneBuildResponse>> runeBuildsByPos =
            championStatsQueryPort.getChampionRuneBuilds(championId, patch, platformId, tier);
        Map<String, List<ChampionSkillBuildResponse>> skillBuildsByPos =
            championStatsQueryPort.getChampionSkillBuilds(championId, patch, platformId, tier);

        List<ChampionPositionStatsResponse> stats = winRates.stream()
            .map(wr -> new ChampionPositionStatsResponse(
                wr.teamPosition(),
                wr.totalWinRate(),
                wr.totalGames(),
                matchupsByPos.getOrDefault(wr.teamPosition(), List.of()),
                itemBuildsByPos.getOrDefault(wr.teamPosition(), List.of()),
                runeBuildsByPos.getOrDefault(wr.teamPosition(), List.of()),
                skillBuildsByPos.getOrDefault(wr.teamPosition(), List.of())
            ))
            .toList();

        return new ChampionStatsResponse(tier, stats);
    }
}
