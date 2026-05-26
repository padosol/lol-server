package com.example.lolserver.summoner.adapter.out.persistence.repository.dsl;

import com.example.lolserver.summoner.adapter.out.persistence.entity.SummonerEntity;

import java.util.List;

public interface SummonerRepositoryCustom {

    List<SummonerEntity> findAllByGameNameAndTagLineAndPlatformId(String gameName, String tagLine, String platformId);
    List<SummonerEntity> findAllByGameNameAndTagLineAndPlatformIdLike(String q, String platformId);
}
