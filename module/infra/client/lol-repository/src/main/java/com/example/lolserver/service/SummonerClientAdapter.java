package com.example.lolserver.service;

import com.example.lolserver.client.summoner.SummonerRestClient;
import com.example.lolserver.client.summoner.model.SummonerVO;
import com.example.lolserver.domain.summoner.application.port.SummonerClientPort;
import com.example.lolserver.domain.summoner.dto.SummonerResponse;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SummonerClientAdapter implements SummonerClientPort {

    private final SummonerRestClient summonerRestClient;

    @Override
    public SummonerResponse getSummoner(String gameName, String tagLine, String region) {
        SummonerVO summonerVO = summonerRestClient.getSummonerByGameNameAndTagLine(
                region, gameName, tagLine);

        if (summonerVO == null) {
            throw new CoreException(
                    ErrorType.NOT_FOUND_USER,
                    "존재하지 않는 유저 입니다. " + gameName
            );
        }

        return SummonerResponse.builder()
                .profileIconId(summonerVO.getProfileIconId())
                .puuid(summonerVO.getPuuid())
                .summonerLevel(summonerVO.getSummonerLevel())
                .platform(region)
                .gameName(summonerVO.getGameName())
                .tagLine(summonerVO.getTagLine())
                .build();
    }
}
