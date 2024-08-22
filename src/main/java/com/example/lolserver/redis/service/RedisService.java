package com.example.lolserver.redis.service;

import com.example.lolserver.redis.model.SummonerRankSession;
import com.example.lolserver.redis.model.SummonerRenewalSession;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface RedisService {
    void addRankData(SummonerRankSession session);

    boolean summonerRenewalStatus(String puuid) throws JsonProcessingException;

}
