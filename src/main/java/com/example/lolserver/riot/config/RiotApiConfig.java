package com.example.lolserver.riot.config;

import com.example.lolserver.riot.core.calling.DefaultRiotExecute;
import com.example.lolserver.riot.core.calling.RiotExecuteProxy;
import com.example.lolserver.riot.core.api.RiotAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class RiotApiConfig {

    @Bean
    RiotAPI riotAPI(RiotAPIProperties properties) {
        return RiotAPI.builder()
                .apiKey(properties.getKey())
                .execute(new RiotExecuteProxy(new DefaultRiotExecute(properties.getKey())))
                .build();
    }

    @Bean
    public Executor executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(100);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("custom-executor");

        return executor;
    }

}
