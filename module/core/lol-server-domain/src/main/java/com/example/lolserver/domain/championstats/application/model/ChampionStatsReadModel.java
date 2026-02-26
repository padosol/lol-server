package com.example.lolserver.domain.championstats.application.model;

import java.util.List;

public record ChampionStatsReadModel(
    String tier,
    List<ChampionPositionStatsReadModel> stats
) {}
