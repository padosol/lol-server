package com.example.lolserver.domain.championstats.application;

import com.example.lolserver.domain.championstats.application.dto.ChampionItemBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionMatchupResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionRuneBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionSkillBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionStatsResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionWinRateResponse;
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChampionStatsService {

    private final ChampionStatsQueryPort championStatsQueryPort;

    public ChampionStatsResponse getChampionStats(int championId, String patch, String platformId) {
        CompletableFuture<List<ChampionWinRateResponse>> winRatesFuture =
            CompletableFuture.supplyAsync(() ->
                championStatsQueryPort.getChampionWinRates(championId, patch, platformId));

        CompletableFuture<List<ChampionMatchupResponse>> matchupsFuture =
            CompletableFuture.supplyAsync(() ->
                championStatsQueryPort.getChampionMatchups(championId, patch, platformId));

        CompletableFuture<List<ChampionItemBuildResponse>> itemBuildsFuture =
            CompletableFuture.supplyAsync(() ->
                championStatsQueryPort.getChampionItemBuilds(championId, patch, platformId));

        CompletableFuture<List<ChampionRuneBuildResponse>> runeBuildsFuture =
            CompletableFuture.supplyAsync(() ->
                championStatsQueryPort.getChampionRuneBuilds(championId, patch, platformId));

        CompletableFuture<List<ChampionSkillBuildResponse>> skillBuildsFuture =
            CompletableFuture.supplyAsync(() ->
                championStatsQueryPort.getChampionSkillBuilds(championId, patch, platformId));

        CompletableFuture.allOf(
            winRatesFuture, matchupsFuture, itemBuildsFuture,
            runeBuildsFuture, skillBuildsFuture
        ).join();

        return new ChampionStatsResponse(
            winRatesFuture.join(),
            matchupsFuture.join(),
            itemBuildsFuture.join(),
            runeBuildsFuture.join(),
            skillBuildsFuture.join()
        );
    }
}
