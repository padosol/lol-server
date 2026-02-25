package com.example.lolserver.domain.championstats.application.port.out;

import com.example.lolserver.domain.championstats.application.dto.ChampionItemBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionMatchupResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionRuneBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionSkillBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionWinRateResponse;

import java.util.List;
import java.util.Map;

public interface ChampionStatsQueryPort {

    List<ChampionWinRateResponse> getChampionWinRates(int championId, String patch, String platformId, String tier);

    Map<String, List<ChampionMatchupResponse>> getChampionMatchups(int championId, String patch, String platformId, String tier);

    Map<String, List<ChampionItemBuildResponse>> getChampionItemBuilds(int championId, String patch, String platformId, String tier);

    Map<String, List<ChampionRuneBuildResponse>> getChampionRuneBuilds(int championId, String patch, String platformId, String tier);

    Map<String, List<ChampionSkillBuildResponse>> getChampionSkillBuilds(int championId, String patch, String platformId, String tier);
}
