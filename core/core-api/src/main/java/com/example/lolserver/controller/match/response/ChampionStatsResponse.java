package com.example.lolserver.controller.match.response;

import com.example.lolserver.storage.db.core.repository.match.dto.MSChampionDTO;

import java.util.List;

public record ChampionStatsResponse(
    List<ChampionStatResponse> soloRankStats,
    List<ChampionStatResponse> flexRankStats,
    List<ChampionStatResponse> totalStats
) {
    public static ChampionStatsResponse of(List<MSChampionDTO> msChampionDTOS) {

        for (MSChampionDTO msChampionDTO : msChampionDTOS) {

        }


        return new ChampionStatsResponse(null, null, null);
    }
}
