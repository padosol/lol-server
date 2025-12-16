package com.example.lolserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService{

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean isUpdating(String puuid) {
        String s = stringRedisTemplate.opsForValue().get(puuid);
        return StringUtils.hasText(s);
    }

    @Override
    public void createSummonerRenewal(String puuid) {
        stringRedisTemplate.opsForValue().set(puuid, puuid);
    }

    @Override
    public boolean isSummonerRenewal(String puuid) {
        String summonerRenewal = stringRedisTemplate.opsForValue().get(puuid);
        return StringUtils.hasText(summonerRenewal);
    }

}
