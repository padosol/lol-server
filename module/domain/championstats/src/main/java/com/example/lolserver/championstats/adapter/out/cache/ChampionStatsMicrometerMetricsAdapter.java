package com.example.lolserver.championstats.adapter.out.cache;

import com.example.lolserver.championstats.application.port.out.ChampionStatsMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChampionStatsMicrometerMetricsAdapter implements ChampionStatsMetricsPort {

    private static final String COUNTER_NAME = "lol.bq.single_flight.fallback";

    private final MeterRegistry meterRegistry;

    @Override
    public void recordSingleFlightFallback(String endpoint) {
        Counter.builder(COUNTER_NAME)
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .increment();
    }
}
