package com.example.lolserver.riot.api.core.summoner;

import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;

public class SummonerAPI {

    private static final SummonerAPI INSTANCE = new SummonerAPI();

    public static SummonerAPI getInstance() {
        return INSTANCE;
    }

    private SummonerAPI() {};

    private static Platform platform;
    private final SummonerBuilder builder = new SummonerBuilder();

    public SummonerDTO byName(Platform platform, String name) {
        return builder.withPlatform(platform).withName(name).get();
    }


}