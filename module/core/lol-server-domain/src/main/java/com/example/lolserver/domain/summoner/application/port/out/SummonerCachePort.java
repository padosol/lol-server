package com.example.lolserver.domain.summoner.application.port.out;

public interface SummonerCachePort {
    boolean isUpdating(String puuid);
    void createSummonerRenewal(String puuid);
    boolean isSummonerRenewal(String puuid);

    /**
     * 분산 락 획득 시도
     * @param key 락 키
     * @return 락 획득 성공 여부
     */
    boolean tryLock(String key);

    /**
     * 분산 락 해제
     * @param key 락 키
     */
    void unlock(String key);
}
