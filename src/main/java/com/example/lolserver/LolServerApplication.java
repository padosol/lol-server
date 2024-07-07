package com.example.lolserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Optional;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class LolServerApplication {

    // jenkins test3

    public static void main(String[] args) {
        SpringApplication.run(LolServerApplication.class, args);
    }


}
