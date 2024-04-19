package com.example.lolserver.web.champion.service;

import com.example.lolserver.riot.api.RiotApi;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.champion.ChampionInfo;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ChampionServiceV1 implements ChampionService{
    @Override
    public ChampionInfo getRotation(String region) throws IOException, InterruptedException {

        ChampionInfo championInfo = RiotApi.champion().rotation(Platform.KOREA).get();

        if(championInfo.isError()) {
            return null;
        }

        return championInfo;
    }
}
