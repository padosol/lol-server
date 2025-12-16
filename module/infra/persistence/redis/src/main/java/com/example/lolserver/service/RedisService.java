package com.example.lolserver.service;

public interface RedisService {
    boolean isUpdating(String puuid);
    void createSummonerRenewal(String puuid);
    boolean isSummonerRenewal(String puuid);

}