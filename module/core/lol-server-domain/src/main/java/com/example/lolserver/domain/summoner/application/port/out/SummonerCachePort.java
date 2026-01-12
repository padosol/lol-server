package com.example.lolserver.domain.summoner.application.port.out;

public interface SummonerCachePort {
    boolean isUpdating(String puuid);
    void createSummonerRenewal(String puuid);
    boolean isSummonerRenewal(String puuid);
}
