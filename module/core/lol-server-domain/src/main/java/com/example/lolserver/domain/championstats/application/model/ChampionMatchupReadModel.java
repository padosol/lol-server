package com.example.lolserver.domain.championstats.application.model;

public record ChampionMatchupReadModel(
    int opponentChampionId,
    long totalGames,
    long totalWins,
    double totalWinRate
) {}
