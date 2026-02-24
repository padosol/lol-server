package com.example.lolserver.domain.championstats.application.dto;

public record ChampionRuneBuildResponse(
    int championId,
    String teamPosition,
    int primaryStyleId,
    String primaryPerkIds,
    int subStyleId,
    String subPerkIds,
    long totalGames,
    long totalWins,
    double totalWinRate
) {}
