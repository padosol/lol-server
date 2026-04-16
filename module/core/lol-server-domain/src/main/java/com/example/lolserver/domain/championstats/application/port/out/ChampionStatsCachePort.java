package com.example.lolserver.domain.championstats.application.port.out;

import com.example.lolserver.domain.championstats.application.model.ChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.PositionChampionStatsReadModel;

import java.util.List;

public interface ChampionStatsCachePort {

    ChampionStatsReadModel findChampionStats(int championId, String patch, String platformId, String tierDisplay);

    void saveChampionStats(int championId, String patch, String platformId,
                           String tierDisplay, ChampionStatsReadModel stats);

    List<PositionChampionStatsReadModel> findChampionStatsByPosition(
            String patch, String platformId, String tierDisplay);

    void saveChampionStatsByPosition(String patch, String platformId,
                                     String tierDisplay,
                                     List<PositionChampionStatsReadModel> stats);
}
