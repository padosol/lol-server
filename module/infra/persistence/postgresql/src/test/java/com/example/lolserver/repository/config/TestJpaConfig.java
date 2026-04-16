package com.example.lolserver.repository.config;

import com.example.lolserver.config.AsyncQueryConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.example.lolserver.repository")
@EnableJpaRepositories(basePackages = "com.example.lolserver.repository")
@ComponentScan(basePackages = "com.example.lolserver.repository")
@Import(AsyncQueryConfig.class)
public class TestJpaConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
