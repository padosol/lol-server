package com.example.lolserver.redis.service;

import com.example.lolserver.redis.model.SummonerRankSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService{

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void addRankData(SummonerRankSession session) {

        ZSetOperations<String, Object> rank = redisTemplate.opsForZSet();
        HashOperations<String, Object, Object> summoner = redisTemplate.opsForHash();

        // 랭킹 정보 등록, sorted set
        // 유저 정보 등록, hash
        if(session.hasKey()) {
            rank.add("rank_" + session.getKey(), session.getSummonerName(), session.getScore());
            summoner.put("summoner_" + session.getKey(), session.getSummonerName(), session);
        }

    }
}
