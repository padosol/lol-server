package com.example.lolserver.domain.championstats.application.dto;

public record ChampionWinRateResponse(
    String teamPosition,
    long totalGames,
    long totalWins,
    double totalWinRate
) {}
