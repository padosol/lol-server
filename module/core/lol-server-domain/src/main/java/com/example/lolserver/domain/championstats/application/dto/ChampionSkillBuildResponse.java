package com.example.lolserver.domain.championstats.application.dto;

public record ChampionSkillBuildResponse(
    String skillOrder15,
    long totalGames,
    long totalWins,
    double totalWinRate
) {}
