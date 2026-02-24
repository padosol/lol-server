package com.example.lolserver.domain.championstats.application.port.out;

import com.example.lolserver.domain.championstats.application.dto.ChampionItemBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionMatchupResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionRuneBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionSkillBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionWinRateResponse;

import java.util.List;

public interface ChampionStatsQueryPort {

    List<ChampionWinRateResponse> getChampionWinRates(int championId, String patch, String platformId);

    List<ChampionMatchupResponse> getChampionMatchups(int championId, String patch, String platformId);

    List<ChampionItemBuildResponse> getChampionItemBuilds(int championId, String patch, String platformId);

    List<ChampionRuneBuildResponse> getChampionRuneBuilds(int championId, String patch, String platformId);

    List<ChampionSkillBuildResponse> getChampionSkillBuilds(int championId, String patch, String platformId);
}
