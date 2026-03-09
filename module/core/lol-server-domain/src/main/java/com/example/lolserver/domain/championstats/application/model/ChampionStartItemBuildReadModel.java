package com.example.lolserver.domain.championstats.application.model;

public record ChampionStartItemBuildReadModel(
    String startItems,
    long games,
    double winRate,
    double pickRate
) {}
