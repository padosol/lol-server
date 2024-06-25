package com.example.lolserver.bucket;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Bucket4jTest {



    @Test
    void BUCKET4j_TEST() throws InterruptedException {

        Refill refill = Refill.intervally(10, Duration.ofSeconds(2));
        Bandwidth limit = Bandwidth.classic(10, refill);

        Bucket bucket = Bucket.builder()
                .addLimit(limit)
                .build();

        bucket.tryConsume(1L);

        System.out.println(bucket.tryConsumeAsMuchAsPossible());

    }

}
