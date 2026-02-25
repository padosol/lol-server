package com.example.lolserver.domain.championstats.application.dto;

import java.util.List;

public record ChampionPositionStatsResponse(
    String teamPosition,
    double winRate,
    long totalCount,
    List<ChampionMatchupResponse> matchups,
    List<ChampionItemBuildResponse> itemBuilds,
    List<ChampionRuneBuildResponse> runeBuilds,
    List<ChampionSkillBuildResponse> skillBuilds
) {}
