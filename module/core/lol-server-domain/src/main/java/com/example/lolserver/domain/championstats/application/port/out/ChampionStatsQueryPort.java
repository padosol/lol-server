package com.example.lolserver.domain.championstats.application.port.out;

import com.example.lolserver.domain.championstats.application.model.ChampionItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionMatchupReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRuneBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSkillBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionWinRateReadModel;

import java.util.List;
import java.util.Map;

public interface ChampionStatsQueryPort {

    List<ChampionWinRateReadModel> getChampionWinRates(int championId, String patch, String platformId, String tier);

    Map<String, List<ChampionMatchupReadModel>> getChampionMatchups(
            int championId, String patch, String platformId, String tier);

    Map<String, List<ChampionItemBuildReadModel>> getChampionItemBuilds(
            int championId, String patch, String platformId, String tier);

    Map<String, List<ChampionRuneBuildReadModel>> getChampionRuneBuilds(
            int championId, String patch, String platformId, String tier);

    Map<String, List<ChampionSkillBuildReadModel>> getChampionSkillBuilds(
            int championId, String patch, String platformId, String tier);
}
