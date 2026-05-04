package com.example.lolserver.domain.championstats.application.model;

public record ChampionBootBuildReadModel(
    int bootId,
    long games,
    double winRate,
    double pickRate
) {}
