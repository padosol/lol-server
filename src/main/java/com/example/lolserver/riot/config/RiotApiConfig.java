package com.example.lolserver.riot.config;

import com.example.lolserver.riot.core.calling.DefaultRiotExecute;
import com.example.lolserver.riot.core.calling.RiotExecuteProxy;
import com.example.lolserver.riot.core.api.RiotAPI;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;


@EnableAsync
@Configuration
public class RiotApiConfig {

    @Bean
    RiotAPI riotAPI(RiotAPIProperties properties, Bucket bucket, RedisTemplate<String, Object> redisTemplate) {
        return RiotAPI.builder()
                .apiKey(properties.getKey())
                .redisTemplate(redisTemplate)
                .execute(new RiotExecuteProxy(new DefaultRiotExecute(properties.getKey(), bucket), bucket, redisTemplate))
                .bucket(bucket)
                .build();
    }

}
