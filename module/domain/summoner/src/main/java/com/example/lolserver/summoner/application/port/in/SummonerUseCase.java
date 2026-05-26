package com.example.lolserver.summoner.application.port.in;

import com.example.lolserver.summoner.domain.SummonerRenewal;

public interface SummonerUseCase {

    SummonerRenewal renewalSummonerInfo(String platformId, String puuid);
}
