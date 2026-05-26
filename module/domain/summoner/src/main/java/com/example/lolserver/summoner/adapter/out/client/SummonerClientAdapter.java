package com.example.lolserver.summoner.adapter.out.client;

import com.example.lolserver.summoner.adapter.out.client.restclient.summoner.SummonerRestClient;
import com.example.lolserver.summoner.adapter.out.client.restclient.summoner.model.SummonerVO;
import com.example.lolserver.summoner.application.port.out.SummonerClientPort;
import com.example.lolserver.summoner.domain.Summoner;
import com.example.lolserver.summoner.adapter.out.client.mapper.SummonerClientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SummonerClientAdapter implements SummonerClientPort {

    private final SummonerRestClient summonerRestClient;
    private final SummonerClientMapper summonerClientMapper;

    @Override
    public Optional<Summoner> getSummoner(String gameName, String tagLine, String platformId) {
        SummonerVO summonerVO = summonerRestClient.getSummonerByGameNameAndTagLine(
                platformId, gameName, tagLine);
        return Optional.ofNullable(summonerVO)
                .map(summonerClientMapper::toDomain);
    }

    @Override
    public Optional<Summoner> getSummonerByPuuid(String platformId, String puuid) {
        SummonerVO summonerVO = summonerRestClient.getSummonerByPuuid(platformId, puuid);
        return Optional.ofNullable(summonerVO)
                .map(summonerClientMapper::toDomain);
    }
}
