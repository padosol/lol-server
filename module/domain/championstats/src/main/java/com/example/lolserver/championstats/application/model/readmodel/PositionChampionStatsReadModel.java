package com.example.lolserver.championstats.application.model.readmodel;

import java.util.List;

public record PositionChampionStatsReadModel(
        String teamPosition,
        List<ChampionRateReadModel> champions
) {}
