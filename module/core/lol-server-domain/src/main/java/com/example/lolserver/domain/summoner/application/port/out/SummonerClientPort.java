package com.example.lolserver.domain.summoner.application.port.out;

import com.example.lolserver.domain.summoner.domain.Summoner;

import java.util.Optional;

public interface SummonerClientPort {
    Optional<Summoner> getSummoner(String gameName, String tagLine, String region);

    Optional<Summoner> getSummonerByPuuid(String region, String puuid);
}
