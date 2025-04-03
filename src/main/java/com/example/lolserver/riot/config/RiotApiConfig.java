package com.example.lolserver.riot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;

import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.core.calling.DefaultRiotExecute;
import com.example.lolserver.riot.core.calling.RiotExecuteProxy;
import com.example.lolserver.web.bucket.BucketService;


@EnableAsync
@Configuration
public class RiotApiConfig {

    @Bean
    RiotAPI riotAPI(RiotAPIProperties properties, BucketService bucketService, RedisTemplate<String, Object> redisTemplate) {
        return RiotAPI.builder()
                .apiKey(properties.getKey())
                .redisTemplate(redisTemplate)
                .execute(new RiotExecuteProxy(new DefaultRiotExecute(properties.getKey()), bucketService))
                .bucket(bucketService)
                .build();
    }

}
