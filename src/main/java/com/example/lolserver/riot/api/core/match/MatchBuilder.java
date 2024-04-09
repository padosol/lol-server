package com.example.lolserver.riot.api.core.match;

import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.match.MatchDto;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class MatchBuilder {

    private Platform platform;
    private String path;
    private MatchQuery query;

    public static class MatchQuery {
        private Long startTime;
        private Long endTime;
        private Integer queue;
        private String type;
        private Integer start = 0;
        private Integer count = 20;

        public void startTime(Long startTime) {
            this.startTime = startTime;
        }

    }

    public MatchBuilder withPlatform(Platform platform) {
        this.platform = platform;
        return this;
    }

    public MatchBuilder withPuuid(String puuid) {
        this.path = MatchPath.BY_PUUID.pathParam(puuid);
        return this;
    }

    public MatchBuilder withMatchId(String matchId) {
        this.path = MatchPath.MATCH.pathParam(matchId);
        return this;
    }

    public MatchBuilder withTimeline(String matchId) {
        this.path = MatchPath.TIMELINE.pathParam(matchId);
        return this;
    }

    public MatchBuilder queryParam(MatchQuery query) {
        this.query = query;

        return this;
    }

    public MatchDto get() {
        return null;
    }

}
