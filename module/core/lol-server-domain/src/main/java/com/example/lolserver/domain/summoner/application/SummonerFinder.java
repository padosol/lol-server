package com.example.lolserver.domain.summoner.application;

import com.example.lolserver.domain.summoner.application.port.SummonerClientPort;
import com.example.lolserver.domain.summoner.application.port.SummonerPersistencePort;
import com.example.lolserver.domain.summoner.dto.SummonerResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SummonerFinder {

    private final SummonerPersistencePort summonerPersistenceAdapter;

    private final SummonerClientPort summonerClientAdapter;

    public SummonerFinder(
            @Qualifier("summonerPersistenceAdapter")
            SummonerPersistencePort summonerPersistenceAdapter,
            @Qualifier("summonerClientAdapter")
            SummonerClientPort summonerClientAdapter) {
        this.summonerPersistenceAdapter = summonerPersistenceAdapter;
        this.summonerClientAdapter = summonerClientAdapter;
    }

    SummonerResponse findSummonerBy(String gameName, String tagLine, String region) {
        SummonerResponse summoner = summonerPersistenceAdapter.getSummoner(gameName, tagLine, region);

        if (summoner == null) {
            summoner = summonerClientAdapter.getSummoner(gameName, tagLine, region);
        }

        return summoner;
    }
}
