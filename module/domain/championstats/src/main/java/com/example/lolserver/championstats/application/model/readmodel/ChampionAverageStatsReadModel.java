package com.example.lolserver.championstats.application.model.readmodel;

public record ChampionAverageStatsReadModel(
    String teamPosition,
    double avgKills,
    double avgDeaths,
    double avgAssists,
    double kda,
    double avgGoldPerMinute,
    double avgLaneCs10m,
    double avgJungleCs10m
) {}
