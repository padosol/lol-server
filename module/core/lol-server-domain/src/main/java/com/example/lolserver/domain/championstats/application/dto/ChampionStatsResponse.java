package com.example.lolserver.domain.championstats.application.dto;

import java.util.List;

public record ChampionStatsResponse(
    String tier,
    List<ChampionPositionStatsResponse> stats
) {}
