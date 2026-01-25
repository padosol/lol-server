package com.example.lolserver.adapter.champion;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "champion.client.fake")
public class FakeChampionClientProperties {

    private boolean enabled = false;
    private long delayMs = 200;
    private int rateLimitRequests = 500;
    private int rateLimitSeconds = 10;
    private RateLimitStrategy rateLimitStrategy = RateLimitStrategy.THROW_EXCEPTION;

    public enum RateLimitStrategy {
        THROW_EXCEPTION,
        BLOCKING_WAIT,
        REJECT_SILENTLY
    }
}
