package com.example.lolserver.redis;

import com.example.lolserver.redis.model.TestSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

@SpringBootTest
public class RedisTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void beforeEach() {
    }

    @Test
    void REDIS_DATA_ID_TEST() {

        TestSession session = new TestSession();
        session.setId("test");
        session.setName("tester");

        TestSession session1 = new TestSession();
        session1.setId("test1");
        session1.setName("tester2");

        ZSetOperations<String, Object> zSet = redisTemplate.opsForZSet();

        HashOperations<String, Object, Object> redisHash = redisTemplate.opsForHash();

        zSet.add("solo", session, 1);
        redisHash.put("solo", session.getId(), session);

        Set<Object> test = zSet.range("test", 0, -1);

        Assertions.assertThat(test.size()).isEqualTo(1);
    }
}