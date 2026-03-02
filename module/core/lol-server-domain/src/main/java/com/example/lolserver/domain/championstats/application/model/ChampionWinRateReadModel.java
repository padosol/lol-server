package com.example.lolserver.domain.championstats.application.model;

public record ChampionWinRateReadModel(
    String teamPosition,
    long totalGames,
    long totalWins,
    double totalWinRate
) {}
