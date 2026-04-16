package com.example.lolserver.domain.championstats.application.port.in;

import com.example.lolserver.TierFilter;
import com.example.lolserver.domain.championstats.application.model.ChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.PositionChampionStatsReadModel;

import java.util.List;

public interface ChampionStatsQueryUseCase {

    ChampionStatsReadModel getChampionStats(int championId, String patch, String platformId, TierFilter tierFilter);

    List<PositionChampionStatsReadModel> getChampionStatsByPosition(
            String patch, String platformId, TierFilter tierFilter);
}
