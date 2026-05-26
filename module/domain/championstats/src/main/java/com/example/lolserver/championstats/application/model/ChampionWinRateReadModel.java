package com.example.lolserver.championstats.application.model;

public record ChampionWinRateReadModel(
    String teamPosition,
    long totalGames,
    long totalWins,
    double totalWinRate
) {}
