package com.example.lolserver.championstats.application.port.in;

import com.example.lolserver.shared.TierFilter;
import com.example.lolserver.championstats.application.model.readmodel.ChampionStatsReadModel;
import com.example.lolserver.championstats.application.model.readmodel.PositionChampionStatsReadModel;

import java.util.List;

public interface ChampionStatsQueryUseCase {

    ChampionStatsReadModel getChampionStats(int championId, String patch, String platformId, TierFilter tierFilter);

    List<PositionChampionStatsReadModel> getChampionStatsByPosition(
            String patch, String platformId, TierFilter tierFilter);
}
