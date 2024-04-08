package com.example.lolserver.riot.api.core.summoner;

import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;

import java.io.IOException;

public class SummonerAPI {

    private static final SummonerAPI INSTANCE = new SummonerAPI();

    public static SummonerAPI getInstance() {
        return INSTANCE;
    }

    private SummonerAPI() {};

    private final SummonerBuilder builder = new SummonerBuilder();

    public SummonerDTO byName(Platform platform, String name) throws IOException, InterruptedException {
        return builder.withPlatform(platform).withName(name).get();
    }

    public SummonerDTO byPuuid(Platform platform, String puuid) throws IOException, InterruptedException {
        return builder.withPlatform(platform).withPuuid(puuid).get();
    }


}