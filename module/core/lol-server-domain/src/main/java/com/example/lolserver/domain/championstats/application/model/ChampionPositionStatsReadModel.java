package com.example.lolserver.domain.championstats.application.model;

import java.util.List;

public record ChampionPositionStatsReadModel(
    String teamPosition,
    double winRate,
    long totalCount,
    List<ChampionMatchupReadModel> matchups,
    List<ChampionItemBuildReadModel> itemBuilds,
    List<ChampionRuneBuildReadModel> runeBuilds,
    List<ChampionSkillBuildReadModel> skillBuilds
) {}
