package com.example.lolserver.summoner.application.port.in;

import com.example.lolserver.summoner.domain.SummonerRenewal;

public interface SummonerUseCase {

    SummonerRenewal renewalSummonerInfo(String platformId, String puuid);

    /**
     * 저장된 소환사의 platformId 로 갱신을 요청한다. 저장되지 않은 소환사면 NOT_FOUND_PUUID.
     */
    SummonerRenewal renewalSummonerInfo(String puuid);
}
