package com.example.lolserver.domain.championstats.application.model;

import java.util.List;

public record ChampionItemBuildReadModel(
    List<Integer> itemBuild,
    long games,
    double winRate,
    double pickRate
) {}
