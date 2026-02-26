package com.example.lolserver.domain.championstats.application.model;

public record ChampionSkillBuildReadModel(
    String skillOrder15,
    long totalGames,
    long totalWins,
    double totalWinRate
) {}
