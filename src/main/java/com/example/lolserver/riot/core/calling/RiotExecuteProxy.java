package com.example.lolserver.riot.core.calling;

import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RiotExecuteProxy implements RiotExecute{

    private RiotExecute execute;
    private Bucket bucket;
    private RedisTemplate<String, Object> redisTemplate;

    public RiotExecuteProxy(RiotExecute execute, Bucket bucket, RedisTemplate<String, Object> redisTemplate) {
        this.execute = execute;
        this.bucket = bucket;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public <T> CompletableFuture<T> execute(Class<T> clazz, URI uri) {

        synchronized (this) {

            log.info("사용가능 토큰 수: {}", bucket.getAvailableTokens());
            if(bucket.tryConsume(1L)) {
                CompletableFuture<T> result = execute.execute(clazz, uri);
                return result;
            } else {
                throw new IllegalStateException("429 Many too request");
            }

        }

    }

    public RedisTemplate<String, Object> getRedisTemplate() {
        return this.redisTemplate;
    }
}
