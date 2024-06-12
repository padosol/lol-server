package com.example.lolserver.redis.config;

import com.example.lolserver.redis.model.RedisSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, RedisSession> redisTemplate(RedisConnectionFactory redisConnectionFactory) {

        RedisTemplate<String, RedisSession> redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(redisConnectionFactory);

        return redisTemplate;
    }

}
