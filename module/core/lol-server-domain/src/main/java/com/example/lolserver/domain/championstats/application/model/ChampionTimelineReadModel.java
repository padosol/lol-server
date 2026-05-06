package com.example.lolserver.domain.championstats.application.model;

import java.util.List;

public record ChampionTimelineReadModel(
    int championId,
    String tier,
    List<PositionTimelineReadModel> positions
) {}
