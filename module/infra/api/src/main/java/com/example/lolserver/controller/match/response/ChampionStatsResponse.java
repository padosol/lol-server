package com.example.lolserver.controller.match.response;


import java.util.List;

public record ChampionStatsResponse(
    List<ChampionStatResponse> soloRankStats,
    List<ChampionStatResponse> flexRankStats,
    List<ChampionStatResponse> totalStats
) {
}
