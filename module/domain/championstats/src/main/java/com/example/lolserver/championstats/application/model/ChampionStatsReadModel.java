package com.example.lolserver.championstats.application.model;

import java.util.List;

public record ChampionStatsReadModel(
    String tier,
    List<ChampionPositionStatsReadModel> positions
) {}
