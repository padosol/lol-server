package com.example.lolserver.riot.core.calling;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RiotExecuteProxy implements RiotExecute{

    private RiotExecute execute;

    public RiotExecuteProxy(RiotExecute execute) {
        this.execute = execute;
    }

    @Override
    public <T> CompletableFuture<T> execute(Class<T> clazz, URI uri) {

        Long start = System.currentTimeMillis();

        CompletableFuture<T> result = execute.execute(clazz, uri);

        Long end = System.currentTimeMillis();

        log.info("API 요청/응답 시간: {}ms", end - start);

        return result;
    }
}
