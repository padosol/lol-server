package com.example.lolserver.domain.championstats.application.dto;

import java.util.List;

public record ChampionStatsResponse(
    List<ChampionWinRateResponse> winRates,
    List<ChampionMatchupResponse> matchups,
    List<ChampionItemBuildResponse> itemBuilds,
    List<ChampionRuneBuildResponse> runeBuilds,
    List<ChampionSkillBuildResponse> skillBuilds
) {}
