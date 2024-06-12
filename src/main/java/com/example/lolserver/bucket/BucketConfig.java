package com.example.lolserver.bucket;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class BucketConfig {

    @Bean
    public Bucket bucket() {

        Refill refill = Refill.intervally(500, Duration.ofSeconds(10));
        Bandwidth limit = Bandwidth.classic(500, refill);

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
