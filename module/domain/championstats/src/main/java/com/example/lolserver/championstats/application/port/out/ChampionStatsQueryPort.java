package com.example.lolserver.championstats.application.port.out;

import com.example.lolserver.shared.TierFilter;
import com.example.lolserver.championstats.application.model.readmodel.ChampionAverageStatsReadModel;
import com.example.lolserver.championstats.application.model.readmodel.ChampionBootBuildReadModel;
import com.example.lolserver.championstats.application.model.readmodel.ChampionItemBuildReadModel;
import com.example.lolserver.championstats.application.model.readmodel.ChampionMatchupReadModel;
import com.example.lolserver.championstats.application.model.readmodel.ChampionRuneBuildReadModel;
import com.example.lolserver.championstats.application.model.readmodel.ChampionSkillBuildReadModel;
import com.example.lolserver.championstats.application.model.readmodel.ChampionSpellStatsReadModel;
import com.example.lolserver.championstats.application.model.readmodel.ChampionStartItemBuildReadModel;
import com.example.lolserver.championstats.application.model.readmodel.ChampionRateReadModel;
import com.example.lolserver.championstats.application.model.readmodel.ChampionWinRateReadModel;

import java.util.List;
import java.util.Map;

public interface ChampionStatsQueryPort {

    Map<String, List<ChampionRateReadModel>> getChampionStatsByPosition(
            String patch, String platformId, TierFilter tierFilter);

    List<ChampionWinRateReadModel> getChampionWinRates(
            int championId, String patch, String platformId, TierFilter tierFilter);

    List<ChampionAverageStatsReadModel> getChampionAverageStats(
            int championId, String patch, String platformId, TierFilter tierFilter);

    List<ChampionMatchupReadModel> getChampionMatchups(
            int championId, String patch, String platformId, TierFilter tierFilter, String position);

    List<ChampionRuneBuildReadModel> getChampionRuneBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position);

    List<ChampionSpellStatsReadModel> getChampionSpellStats(
            int championId, String patch, String platformId, TierFilter tierFilter, String position);

    List<ChampionSkillBuildReadModel> getChampionSkillBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position);

    List<ChampionStartItemBuildReadModel> getChampionStartItemBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position);

    List<ChampionBootBuildReadModel> getChampionBootBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position);

    List<ChampionItemBuildReadModel> getChampionItemBuilds(
            int championId, String patch, String platformId, TierFilter tierFilter, String position);
}
