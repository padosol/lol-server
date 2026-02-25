package com.example.lolserver.domain.championstats.application.dto;

public record ChampionMatchupResponse(
    int opponentChampionId,
    long totalGames,
    long totalWins,
    double totalWinRate
) {}
