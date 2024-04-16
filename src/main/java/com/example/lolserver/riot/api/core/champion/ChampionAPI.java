package com.example.lolserver.riot.api.core.champion;

import com.example.lolserver.riot.api.core.match.MatchAPI;
import com.example.lolserver.riot.api.core.match.MatchBuilder;
import com.example.lolserver.riot.api.core.match.MatchListBuilder;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.match.MatchDto;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class ChampionAPI {

    private static final ChampionAPI INSTANCE = new ChampionAPI();

    public static ChampionAPI getInstance() {
        return INSTANCE;
    }
    private ChampionAPI(){};

    private final ChampionBuilder championBuilder = new ChampionBuilder();

    public ChampionBuilder rotation(Platform platform) {
        return championBuilder.withPlatform(platform);
    }


}
