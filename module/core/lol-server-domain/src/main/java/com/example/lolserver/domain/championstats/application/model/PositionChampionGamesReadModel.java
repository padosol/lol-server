package com.example.lolserver.domain.championstats.application.model;

import java.util.List;

public record PositionChampionGamesReadModel(
        String teamPosition,
        List<ChampionTotalGamesReadModel> champions
) {}
