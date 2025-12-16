package com.example.lolserver.domain.summoner;

import com.example.lolserver.client.summoner.model.SummonerVO;
import com.example.lolserver.domain.summoner.dto.SummonerResponse;
import com.example.lolserver.repository.summoner.entity.SummonerEntity;

public class SummonerMapper {
    public static SummonerEntity voToEntity(SummonerVO summonerVO) {
        return SummonerEntity.builder()
                .profileIconId(summonerVO.getProfileIconId())
                .puuid(summonerVO.getPuuid())
                .summonerLevel(summonerVO.getSummonerLevel())
                .gameName(summonerVO.getGameName())
                .tagLine(summonerVO.getTagLine())
                .revisionDate(summonerVO.getRevisionDate())
                .revisionClickDate(summonerVO.getRevisionDate())
                .build();
    }
}
