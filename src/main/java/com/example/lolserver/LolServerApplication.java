package com.example.lolserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Optional;

@EnableScheduling
@SpringBootApplication
public class LolServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LolServerApplication.class, args);
    }


    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of()
    }

}
