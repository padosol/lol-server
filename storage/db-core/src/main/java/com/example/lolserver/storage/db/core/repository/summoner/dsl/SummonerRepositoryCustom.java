package com.example.lolserver.storage.db.core.repository.summoner.dsl;

import com.example.lolserver.storage.db.core.repository.summoner.entity.Summoner;

import java.util.List;

public interface SummonerRepositoryCustom {

    Summoner findByGameNameAndTagLine(String gameName, String tagLine);
    List<Summoner> findAllByGameNameAndTagLineAndRegion(String gameName, String tagLine, String region);
    List<Summoner> findAllByGameNameAndTagLineAndRegionLike(String gameName, String tagLine, String region);
}
