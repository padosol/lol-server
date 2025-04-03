package com.example.lolserver.bucket;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.SynchronizationStrategy;

@Configuration
public class BucketConfig {

    @Bean
    public Bucket bucket() {

        Refill refill = Refill.intervally(490, Duration.ofSeconds(10));
        Bandwidth limit = Bandwidth.classic(490, refill);

        return Bucket.builder()
                .addLimit(limit)
                .withSynchronizationStrategy(SynchronizationStrategy.SYNCHRONIZED)
                .build();
    }
}
