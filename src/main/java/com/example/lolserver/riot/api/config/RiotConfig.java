package com.example.lolserver.riot.api.config;

import com.example.lolserver.riot.api.RiotApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RiotConfig {

    @Bean
    public RiotApi riotApi() {
        return new RiotApi();
    }

}