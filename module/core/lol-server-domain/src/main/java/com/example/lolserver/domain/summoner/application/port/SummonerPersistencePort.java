package com.example.lolserver.domain.summoner.application.port;

import com.example.lolserver.domain.summoner.dto.SummonerResponse;

public interface SummonerPersistencePort {
    SummonerResponse getSummoner(String gameName, String tagLine, String region);
}
