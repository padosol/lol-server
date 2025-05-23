package com.example.lolserver.riot.core.calling;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import com.example.lolserver.web.bucket.BucketService;

import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RiotExecuteProxy implements RiotExecute{

    private final RiotExecute execute;
    private final BucketService bucketService;

    public RiotExecuteProxy(RiotExecute execute, BucketService bucketService) {
        this.execute = execute;
        this.bucketService = bucketService;
    }

    @Override
    public <T> CompletableFuture<T> execute(Class<T> clazz, URI uri) {

        Bucket bucket = bucketService.getBucket();

        if(bucket.tryConsume(1L)) {
            return execute.execute(clazz, uri);
        }

        return null;
    }
}
