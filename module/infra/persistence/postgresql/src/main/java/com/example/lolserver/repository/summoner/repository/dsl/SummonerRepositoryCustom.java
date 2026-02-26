package com.example.lolserver.repository.summoner.repository.dsl;

import com.example.lolserver.repository.summoner.entity.SummonerEntity;

import java.util.List;

public interface SummonerRepositoryCustom {

    List<SummonerEntity> findAllByGameNameAndTagLineAndPlatformId(String gameName, String tagLine, String platformId);
    List<SummonerEntity> findAllByGameNameAndTagLineAndPlatformIdLike(String q, String platformId);
}
