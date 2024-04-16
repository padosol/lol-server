package com.example.lolserver.web.summoner.repository.dsl;

import com.example.lolserver.web.summoner.entity.Summoner;

import java.util.List;

public interface SummonerRepositoryCustom {
    List<Summoner> findAllByGameNameAndTagLine(Summoner summoner);

    List<Summoner> findAllByGameName(Summoner summoner);
}
