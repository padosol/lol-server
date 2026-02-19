package com.example.lolserver.domain.summoner.application.port.out;

import com.example.lolserver.domain.summoner.domain.Summoner;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SummonerPersistencePort {
    Optional<Summoner> getSummoner(String gameName, String tagLine, String region);

    List<Summoner> getSummonerAuthComplete(String q, String region);

    Optional<Summoner> findById(String puuid);

    Summoner save(Summoner summoner);

    List<Summoner> findAllByPuuidIn(Collection<String> puuids);
}
