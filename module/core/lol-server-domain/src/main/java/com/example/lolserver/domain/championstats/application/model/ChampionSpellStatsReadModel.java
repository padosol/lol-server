package com.example.lolserver.domain.championstats.application.model;

public record ChampionSpellStatsReadModel(
    int summoner1Id,
    int summoner2Id,
    long games,
    double winRate,
    double pickRate
) {}
