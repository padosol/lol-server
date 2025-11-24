package com.example.lolserver.storage.redis.service;

import com.example.lolserver.storage.redis.model.SummonerRankSession;
import com.example.lolserver.storage.redis.model.SummonerRenewalSession;

public interface RedisService {
    void addRankData(SummonerRankSession session);

    SummonerRenewalSession summonerRenewalStatus(String puuid);

}