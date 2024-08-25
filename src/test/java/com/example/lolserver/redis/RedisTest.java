package com.example.lolserver.redis;

import com.example.lolserver.redis.model.SummonerRenewalSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;


import java.util.Set;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
public class RedisTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Test
    void REDIS_HASH_PUUID_ADD() throws JsonProcessingException {
        HashOperations<String, Object, Object> redisHash = redisTemplate.opsForHash();

        SummonerRenewalSession summonerRenewalSession = new SummonerRenewalSession();
        summonerRenewalSession.setSummonerUpdate(false);
        summonerRenewalSession.setLeagueUpdate(false);
        summonerRenewalSession.setAccountUpdate(false);
        summonerRenewalSession.setMatchUpdate(false);

        String before = objectMapper.writeValueAsString(summonerRenewalSession);

    }

    @Test
    void REDIS_없는_데이터_조회() {

        HashOperations<String, Object, Object> redisHash = redisTemplate.opsForHash();

        String test = (String)redisHash.get("renewal", "testset");

        Assertions.assertThat(test).isNull();
    }


}