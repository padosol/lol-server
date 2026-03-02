package com.example.lolserver.domain.championstats.application.model;

public record ChampionItemBuildReadModel(
    String itemsSorted,
    long totalGames,
    long totalWins,
    double totalWinRate
) {}
