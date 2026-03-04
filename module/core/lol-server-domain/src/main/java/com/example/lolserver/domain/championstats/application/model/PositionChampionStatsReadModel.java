package com.example.lolserver.domain.championstats.application.model;

import java.util.List;

public record PositionChampionStatsReadModel(
        String teamPosition,
        List<ChampionRateReadModel> champions
) {}
