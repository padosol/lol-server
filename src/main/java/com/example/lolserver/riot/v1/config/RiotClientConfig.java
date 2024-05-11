package com.example.lolserver.riot.v1.config;

import com.example.lolserver.riot.v1.template.RiotTemplate;
import com.example.lolserver.riot.v1.template.impl.DefaultRiotTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RiotClientConfig {


    @Bean
    public RiotTemplate riotTemplate() {
        return new DefaultRiotTemplate();
    }



}
