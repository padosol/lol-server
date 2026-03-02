package com.example.lolserver.domain.championstats.application.model;

public record ChampionRuneBuildReadModel(
    int primaryStyleId,
    String primaryPerkIds,
    int subStyleId,
    String subPerkIds,
    long totalGames,
    long totalWins,
    double totalWinRate
) {}
