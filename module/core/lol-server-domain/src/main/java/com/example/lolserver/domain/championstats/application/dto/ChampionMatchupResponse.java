package com.example.lolserver.domain.championstats.application.dto;

public record ChampionMatchupResponse(
    int championId,
    int opponentChampionId,
    String teamPosition,
    long totalGames,
    long totalWins,
    double totalWinRate
) {}
