package com.example.lolserver.redis.service;

import com.example.lolserver.redis.model.SummonerRankSession;

public interface RedisService {
    void addRankData(SummonerRankSession session);

}
