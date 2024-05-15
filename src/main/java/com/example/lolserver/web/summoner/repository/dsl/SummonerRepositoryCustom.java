package com.example.lolserver.web.summoner.repository.dsl;

import com.example.lolserver.web.summoner.entity.Summoner;

import java.util.List;

public interface SummonerRepositoryCustom {

    Summoner findByGameNameAndTagLine(String gameName, String tagLine);

    List<Summoner> findAllByGameNameAndTagLineAndRegion(String gameName, String tagLine, String region);
}
