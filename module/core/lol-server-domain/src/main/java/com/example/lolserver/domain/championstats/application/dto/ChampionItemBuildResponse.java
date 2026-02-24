package com.example.lolserver.domain.championstats.application.dto;

public record ChampionItemBuildResponse(
    int championId,
    String teamPosition,
    String itemsSorted,
    long totalGames,
    long totalWins,
    double totalWinRate
) {}
