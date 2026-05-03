package com.example.lolserver.domain.championstats.application.model;

import java.util.List;

public record ChampionStartItemBuildReadModel(
    List<Integer> startItems,
    long games,
    double winRate,
    double pickRate
) {}
