package com.example.lolserver.riot.core.builder.match;

import com.example.lolserver.redis.model.MatchSession;
import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.match_timeline.TimelineDto;
import com.example.lolserver.riot.type.Platform;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class TimeLine {

    private Platform platform;

    public TimeLine(Platform platform) {
        this.platform = platform;
    }

    public static class Builder{
        private Platform platform;
        private String matchId;
        private List<String> matchIds;

        public Builder(String matchId) {
            this.matchId = matchId;
        }

        public Builder(List<String> matchIds) {
            this.matchIds = matchIds;
        }

        public TimeLine.Builder platform(Platform platform) {
            this.platform = platform;
            return this;
        }

        public TimelineDto get()  {

            try {
                return get(this.matchId).get();
            } catch(ExecutionException | InterruptedException e) {
                e.printStackTrace();
                log.info("API ERROR");
                return null;
            }

        }

        private CompletableFuture<TimelineDto> get(String matchId) {

            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            builder.scheme("https").host(this.platform.getPlatform() + ".api.riotgames.com");
            builder.path("/lol/match/v5/matches/" + matchId + "/timeline");

            return RiotAPI.getExecute().execute(TimelineDto.class, builder.build().toUri());
        }

    }

    public CompletableFuture<MatchDto> byMatchIdFuture(String matchId) {
        return new Match.Builder(matchId).platform(this.platform).getFuture(matchId);
    }

    public TimelineDto byMatchId(String matchId) {
        return new TimeLine.Builder(matchId).platform(this.platform).get();
    }

}
