package com.example.lolserver.championstats.application.model;

import java.util.List;

public record PositionChampionStatsReadModel(
        String teamPosition,
        List<ChampionRateReadModel> champions
) {}
