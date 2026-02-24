package com.example.lolserver.adapter.champion;

import com.example.lolserver.domain.champion.application.port.out.ChampionClientPort;
import com.example.lolserver.domain.champion.domain.ChampionRotate;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "champion.client.fake.enabled", havingValue = "true")
@EnableConfigurationProperties(FakeChampionClientProperties.class)
public class FakeChampionClientAdapter implements ChampionClientPort {

    private static final List<Integer> FAKE_FREE_CHAMPION_IDS = List.of(
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
    );
    private static final List<Integer> FAKE_FREE_CHAMPION_IDS_FOR_NEW_PLAYERS = List.of(
            18, 19, 20, 21, 22, 23, 24, 25, 26, 27
    );
    private static final int FAKE_MAX_NEW_PLAYER_LEVEL = 10;

    private final FakeChampionClientProperties properties;
    private final Bucket bucket;

    public FakeChampionClientAdapter(FakeChampionClientProperties properties) {
        this.properties = properties;
        this.bucket = createBucket(properties);
        log.info("FakeChampionClientAdapter initialized - delayMs: {}, rateLimit: {}/{} seconds, strategy: {}",
                properties.getDelayMs(),
                properties.getRateLimitRequests(),
                properties.getRateLimitSeconds(),
                properties.getRateLimitStrategy());
    }

    private Bucket createBucket(FakeChampionClientProperties properties) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(properties.getRateLimitRequests())
                .refillGreedy(properties.getRateLimitRequests(), Duration.ofSeconds(properties.getRateLimitSeconds()))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Override
    public ChampionRotate getChampionRotate(String platformId) {
        handleRateLimit();
        simulateDelay();
        return createFakeChampionRotate();
    }

    private void handleRateLimit() {
        switch (properties.getRateLimitStrategy()) {
            case THROW_EXCEPTION -> {
                if (!bucket.tryConsume(1)) {
                    throw new RateLimitExceededException(
                            String.format("Rate limit exceeded: %d requests per %d seconds",
                                    properties.getRateLimitRequests(),
                                    properties.getRateLimitSeconds()));
                }
            }
            case BLOCKING_WAIT -> {
                try {
                    bucket.asBlocking().consume(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Rate limit waiting interrupted", e);
                }
            }
            case REJECT_SILENTLY -> bucket.tryConsume(1);
            default -> throw new IllegalArgumentException(
                "Unknown rate limit strategy: " + properties.getRateLimitStrategy());
        }
    }

    private void simulateDelay() {
        if (properties.getDelayMs() > 0) {
            try {
                Thread.sleep(properties.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Delay simulation interrupted", e);
            }
        }
    }

    private ChampionRotate createFakeChampionRotate() {
        return new ChampionRotate(
                FAKE_MAX_NEW_PLAYER_LEVEL,
                FAKE_FREE_CHAMPION_IDS_FOR_NEW_PLAYERS,
                FAKE_FREE_CHAMPION_IDS
        );
    }
}
