package com.example.lolserver.riot.api.core.match;

import com.example.lolserver.riot.api.type.Platform;

public class MatchBuilder {

    private Platform platform;
    private String path;


    public MatchBuilder withPlatform(Platform platform) {
        return this;
    }

    public MatchBuilder withPuuid(String puuid) {
        return this;
    }

    public MatchBuilder withMatchId(String matchId) {
        return this;
    }


    public MatchBuilder withTimeline(String matchId) {
        return this;
    }

}
