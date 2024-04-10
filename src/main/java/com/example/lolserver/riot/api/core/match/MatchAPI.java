package com.example.lolserver.riot.api.core.match;

import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.match.MatchDto;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class MatchAPI {

    private static final MatchAPI INSTANCE = new MatchAPI();

    public static MatchAPI getInstance() {
        return INSTANCE;
    }
    private MatchAPI(){};

    private final MatchBuilder matchBuilder = new MatchBuilder();
    private final MatchListBuilder matchListBuilder = new MatchListBuilder();
    public MatchListBuilder byPuuid(Platform platform, String puuid) throws UnsupportedEncodingException {
        return matchListBuilder.withPlatform(platform).withPuuid(puuid);
    }

    public List<MatchDto> allMatches(Platform platform, List<String> matchIds) throws IOException, InterruptedException {
        return matchBuilder.getAllMatches(platform, matchIds);
    }

}
