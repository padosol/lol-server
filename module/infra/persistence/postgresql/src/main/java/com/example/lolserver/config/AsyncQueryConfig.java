package com.example.lolserver.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AsyncQueryConfig {

    @Bean("queryExecutor")
    public Executor queryExecutor() {
        Executor vtExecutor = Executors.newVirtualThreadPerTaskExecutor();
        return mdcDelegatingExecutor(vtExecutor);
    }

    private Executor mdcDelegatingExecutor(Executor delegate) {
        return runnable -> {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            delegate.execute(() -> {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                try {
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            });
        };
    }
}
