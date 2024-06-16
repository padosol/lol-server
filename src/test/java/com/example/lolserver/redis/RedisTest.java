package com.example.lolserver.redis;

import com.example.lolserver.redis.model.RedisSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SpringBootTest
public class RedisTest {


    @Autowired
    private RedisTemplate<String, RedisSession> redisTemplate;


    @AfterEach
    void afterEach() {
        redisTemplate.delete("test");
    }

    @Test
    void REDIS_INSERT_TEST() {

        ValueOperations<String, RedisSession> valueOperations = redisTemplate.opsForValue();

        RedisSession redisSession = new RedisSession();
        redisSession.setId("1");
        redisSession.setExpirationInSeconds(1L);

        valueOperations.set("test", redisSession);

        RedisSession findRedisData = valueOperations.get("test");

        Assertions.assertThat("1").isEqualTo(findRedisData.getId());
    }



}
