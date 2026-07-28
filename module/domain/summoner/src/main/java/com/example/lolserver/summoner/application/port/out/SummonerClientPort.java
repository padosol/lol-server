package com.example.lolserver.summoner.application.port.out;

import com.example.lolserver.summoner.domain.Summoner;

import java.util.Optional;

public interface SummonerClientPort {
    Optional<Summoner> getSummoner(String gameName, String tagLine, String platformId);

    Optional<Summoner> getSummonerByPuuid(String platformId, String puuid);
}
