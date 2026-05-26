package com.example.lolserver.gamedata.adapter.out.client;

import com.example.lolserver.gamedata.application.port.out.ChampionClientPort;
import com.example.lolserver.gamedata.domain.ChampionRotate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "champion.client.fake.enabled", havingValue = "false", matchIfMissing = true)
public class ChampionClientAdapter implements ChampionClientPort {

    private final ChampionRotateRestClient championRotateRestClient;
    private final ChampionClientMapper championClientMapper;

    @Override
    public ChampionRotate getChampionRotate(String platformId) {
        ChampionInfo championInfo = championRotateRestClient.getChampionInfo(platformId);
        return championClientMapper.toDomain(championInfo);
    }
}
