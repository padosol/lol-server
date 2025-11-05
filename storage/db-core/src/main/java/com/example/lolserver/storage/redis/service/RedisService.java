package com.example.lolserver.storage.redis.service;

import com.example.lolserver.storage.redis.model.SummonerRankSession;

public interface RedisService {
    void addRankData(SummonerRankSession session);

    boolean summonerRenewalStatus(String puuid);

}