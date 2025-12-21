package com.example.lolserver.domain.summoner.application.port;

import com.example.lolserver.domain.summoner.dto.SummonerResponse;

public interface SummonerClientPort {
    SummonerResponse getSummoner(String gameName, String tagLine, String region);
}
