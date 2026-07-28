package com.example.lolserver.championstats.application.port.out;

public interface ChampionStatsMetricsPort {

    String ENDPOINT_DETAIL = "detail";
    String ENDPOINT_POSITIONS = "positions";

    void recordSingleFlightFallback(String endpoint);
}
