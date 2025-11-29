package com.example.lolserver.storage.redis.service;

public interface RedisService {
    boolean isUpdating(String puuid);
    void createSummonerRenewal(String puuid);
    boolean isSummonerRenewal(String puuid);

}