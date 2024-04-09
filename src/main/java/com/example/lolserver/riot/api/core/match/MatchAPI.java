package com.example.lolserver.riot.api.core.match;

import com.example.lolserver.riot.api.core.summoner.SummonerBuilder;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.match.MatchDto;

public class MatchAPI {

    private static final MatchAPI INSTANCE = new MatchAPI();

    public static MatchAPI getInstance() {
        return INSTANCE;
    }
    private MatchAPI(){};

    private MatchBuilder builder = new MatchBuilder();


    public MatchDto byPuuid(Platform platform, String puuid) {
        return null;
    }






}
