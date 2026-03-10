package com.example.lolserver.domain.championstats.application.model;

public record ChampionItemBuildReadModel(
    String itemBuild,
    long games,
    double winRate,
    double pickRate
) {}
