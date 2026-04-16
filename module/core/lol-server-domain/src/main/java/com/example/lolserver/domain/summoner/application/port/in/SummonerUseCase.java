package com.example.lolserver.domain.summoner.application.port.in;

import com.example.lolserver.domain.summoner.domain.SummonerRenewal;

public interface SummonerUseCase {

    SummonerRenewal renewalSummonerInfo(String platformId, String puuid);
}
