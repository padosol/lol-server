package com.example.lolserver.adapter.champion;

import com.example.lolserver.restclient.summoner.ChampionRotateRestClient;
import com.example.lolserver.restclient.summoner.model.ChampionInfo;
import com.example.lolserver.domain.champion.application.port.out.ChampionClientPort;
import com.example.lolserver.domain.champion.domain.ChampionRotate;
import com.example.lolserver.mapper.champion.ChampionClientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChampionClientAdapter implements ChampionClientPort {

    private final ChampionRotateRestClient championRotateRestClient;
    private final ChampionClientMapper championClientMapper;

    @Override
    public ChampionRotate getChampionRotate(String region) {
        ChampionInfo championInfo = championRotateRestClient.getChampionInfo(region);
        return championClientMapper.toDomain(championInfo);
    }
}
