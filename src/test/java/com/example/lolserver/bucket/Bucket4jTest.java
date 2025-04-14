package com.example.lolserver.bucket;

import com.example.lolserver.web.bucket.BucketService;
import io.github.bucket4j.*;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;


@SpringBootTest(
        properties = {
                "spring.datasource.username=postgres",
                "spring.datasource.password=1234",
                "spring.data.redis.host=localhost",
                "spring.data.redis.port=6379"
        }
)
public class Bucket4jTest {

    public static int apiFailCount = 0;


    @Autowired
    private Supplier<BucketConfiguration> bucketConfiguration;

    @Autowired
    private ProxyManager<String> proxyManager;

    @Autowired
    private BucketService bucketService;

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

    @Test
    void BUCKET4J_REDIS_TEST() {

        BucketProxy bucketProxy = proxyManager.getProxy("riot-api", bucketConfiguration);

        boolean b = bucketProxy.tryConsume(1L);

        Assertions.assertThat(b).isTrue();
    }

    @Test
    void BUCKET4J_REDIS_MULTI_THREAD_TEST() throws InterruptedException {

        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);

        ExecutorService executorService = Executors.newFixedThreadPool(32);

        for( int i=0; i<threadCount; i++) {
            executorService.execute(() -> {
                try {
                    Bucket bucket = bucketService.getBucket();

                    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

                    if(probe.isConsumed()) {
                        System.out.println("소비후 남은 토큰 수: " + probe.getRemainingTokens());
                        System.out.println(Thread.currentThread().getName());
                    }

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        Assertions.assertThat(bucketService.getBucket().getAvailableTokens()).isEqualTo(15);
    }

    @Test
    void BUCKET4J_NONE_REDIS_TEST() throws InterruptedException {

        Bandwidth limit = Bandwidth.builder()
                .capacity(100)
                .refillGreedy(100, Duration.ofSeconds(10))
                .build();

        Bucket bucket = Bucket.builder()
                .addLimit(limit)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(50);

        for(int i=0; i<50; i++) {
            executorService.submit(() -> {
                bucket.tryConsume(1L);
                latch.countDown();
            });
        }

        latch.await();
        executorService.shutdown();

        Assertions.assertThat(bucket.getAvailableTokens()).isEqualTo(50);
    }

}
