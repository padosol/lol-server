package com.example.lolserver.repository.summoner.dsl;

import com.example.lolserver.repository.summoner.dto.SummonerAutoDTO;
import com.example.lolserver.repository.summoner.entity.SummonerEntity;

import java.util.List;

public interface SummonerRepositoryCustom {

    List<SummonerEntity> findAllByGameNameAndTagLineAndRegion(String gameName, String tagLine, String region);
    List<SummonerAutoDTO> findAllByGameNameAndTagLineAndRegionLike(String q, String region);
}
