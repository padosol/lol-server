package com.example.lolserver.adapter.summoner;

import com.example.lolserver.restclient.summoner.SummonerRestClient;
import com.example.lolserver.restclient.summoner.model.SummonerVO;
import com.example.lolserver.domain.summoner.application.port.out.SummonerClientPort;
import com.example.lolserver.domain.summoner.domain.Summoner;
import com.example.lolserver.mapper.summoner.SummonerClientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SummonerClientAdapter implements SummonerClientPort {

    private final SummonerRestClient summonerRestClient;
    private final SummonerClientMapper summonerClientMapper;

    @Override
    public Optional<Summoner> getSummoner(String gameName, String tagLine, String region) {
        SummonerVO summonerVO = summonerRestClient.getSummonerByGameNameAndTagLine(
                region, gameName, tagLine);
        return Optional.ofNullable(summonerVO)
                .map(summonerClientMapper::toDomain);
    }

    @Override
    public Optional<Summoner> getSummonerByPuuid(String region, String puuid) {
        SummonerVO summonerVO = summonerRestClient.getSummonerByPuuid(region, puuid);
        return Optional.ofNullable(summonerVO)
                .map(summonerClientMapper::toDomain);
    }
}
