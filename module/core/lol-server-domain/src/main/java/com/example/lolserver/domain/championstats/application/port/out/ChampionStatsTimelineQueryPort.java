package com.example.lolserver.domain.championstats.application.port.out;

import com.example.lolserver.TierFilter;
import com.example.lolserver.domain.championstats.application.model.TimelineFrameReadModel;

import java.util.List;
import java.util.Map;

public interface ChampionStatsTimelineQueryPort {

    Map<String, List<TimelineFrameReadModel>> aggregateChampionTimeline(
            int championId, String patch, String platformId, TierFilter tierFilter);
}
