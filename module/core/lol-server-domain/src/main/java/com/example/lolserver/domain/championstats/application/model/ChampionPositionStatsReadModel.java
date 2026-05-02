package com.example.lolserver.domain.championstats.application.model;

import java.util.List;

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
    List<ChampionBootBuildReadModel> bootBuilds,
    List<ChampionItemBuildReadModel> itemBuilds
) {}
