package com.example.lolserver.domain.championstats.application.port.out;

import com.example.lolserver.domain.championstats.application.model.ChampionItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionItemStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionMatchupReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRuneBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSkillBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSpellStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionStartItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRateReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionWinRateReadModel;

import java.util.List;
import java.util.Map;

public interface ChampionStatsQueryPort {

    Map<String, List<ChampionRateReadModel>> getChampionStatsByPosition(
            String patch, String platformId, String tier);

    List<ChampionWinRateReadModel> getChampionWinRates(
            int championId, String patch, String platformId, String tier);

    List<ChampionMatchupReadModel> getStrongMatchups(
            int championId, String patch, String platformId, String tier, String position);

    List<ChampionMatchupReadModel> getWeakMatchups(
            int championId, String patch, String platformId, String tier, String position);

    List<ChampionRuneBuildReadModel> getChampionRuneBuilds(
            int championId, String patch, String platformId, String tier, String position);

    List<ChampionSpellStatsReadModel> getChampionSpellStats(
            int championId, String patch, String platformId, String tier, String position);

    List<ChampionSkillBuildReadModel> getChampionSkillBuilds(
            int championId, String patch, String platformId, String tier, String position);

    List<ChampionStartItemBuildReadModel> getChampionStartItemBuilds(
            int championId, String patch, String platformId, String tier, String position);

    List<ChampionItemBuildReadModel> getChampionItemBuilds(
            int championId, String patch, String platformId, String tier, String position);

    List<ChampionItemStatsReadModel> getChampionItemStats(
            int championId, String patch, String platformId, String tier, String position, int itemOrder);
}
