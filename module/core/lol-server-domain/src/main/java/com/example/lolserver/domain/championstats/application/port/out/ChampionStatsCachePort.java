package com.example.lolserver.domain.championstats.application.port.out;

import com.example.lolserver.domain.championstats.application.model.ChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionTimelineReadModel;
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

    ChampionTimelineReadModel findChampionTimeline(
            int championId, String patch, String platformId, String tierDisplay);

    void saveChampionTimeline(int championId, String patch, String platformId,
                              String tierDisplay, ChampionTimelineReadModel timeline);

    boolean tryLockDetail(int championId, String patch, String platformId, String tierDisplay);

    void unlockDetail(int championId, String patch, String platformId, String tierDisplay);

    boolean tryLockByPosition(String patch, String platformId, String tierDisplay);

    void unlockByPosition(String patch, String platformId, String tierDisplay);

    boolean tryLockTimeline(int championId, String patch, String platformId, String tierDisplay);

    void unlockTimeline(int championId, String patch, String platformId, String tierDisplay);
}
