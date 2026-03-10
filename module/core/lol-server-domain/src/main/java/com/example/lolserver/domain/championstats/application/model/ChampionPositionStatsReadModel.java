package com.example.lolserver.domain.championstats.application.model;

import java.util.List;
import java.util.Map;

public record ChampionPositionStatsReadModel(
    String teamPosition,
    double winRate,
    long totalGames,
    List<ChampionMatchupReadModel> strongMatchups,
    List<ChampionMatchupReadModel> weakMatchups,
    List<ChampionRuneBuildReadModel> runeBuilds,
    List<ChampionSpellStatsReadModel> spellStats,
    List<ChampionSkillBuildReadModel> skillBuilds,
    List<ChampionStartItemBuildReadModel> startItemBuilds,
    List<ChampionItemBuildReadModel> itemBuilds,
    Map<Integer, List<ChampionItemStatsReadModel>> itemStatsByOrder
) {}
