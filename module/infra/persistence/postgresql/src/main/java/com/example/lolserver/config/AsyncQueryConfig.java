package com.example.lolserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AsyncQueryConfig {

    @Bean("queryExecutor")
    public Executor queryExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
