package com.example.lolserver.domain.championstats.application.dto;

public record ChampionItemBuildResponse(
    String itemsSorted,
    long totalGames,
    long totalWins,
    double totalWinRate
) {}
