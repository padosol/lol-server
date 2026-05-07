package com.example.lolserver.domain.championstats.application.port.out;

public interface ChampionStatsMetricsPort {

    String ENDPOINT_DETAIL = "detail";
    String ENDPOINT_POSITIONS = "positions";
    String ENDPOINT_TIMELINE = "timeline";

    void recordSingleFlightFallback(String endpoint);
}
