package com.example.lolserver.championstats.application.port.out;

import com.example.lolserver.championstats.application.model.readmodel.ChampionStatsReadModel;
import com.example.lolserver.championstats.application.model.readmodel.PositionChampionStatsReadModel;

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

    boolean tryLockDetail(int championId, String patch, String platformId, String tierDisplay);

    void unlockDetail(int championId, String patch, String platformId, String tierDisplay);

    boolean tryLockByPosition(String patch, String platformId, String tierDisplay);

    void unlockByPosition(String patch, String platformId, String tierDisplay);
}
