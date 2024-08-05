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


        // 수정 필요함
        private List<TimelineDto> getAll()  {

            List<CompletableFuture<TimelineDto>> timelineList = new ArrayList<>();

            Bucket bucket = RiotAPI.getBucket();

            int i = 0;
            for(String matchId : this.matchIds) {

                if( i > 20 ) {
                    ZSetOperations<String, Object> matchSet = RiotAPI.getRedistemplate().opsForZSet();

                    matchSet.add("timeline_matchId", new MatchSession(matchId, this.platform), (double) System.currentTimeMillis() / 1000);
                    continue;
                }

                if(bucket.getAvailableTokens() > 0) {
                    CompletableFuture<TimelineDto> future = get(matchId);
                    timelineList.add(future);
                } else {
                    ZSetOperations<String, Object> matchSet = RiotAPI.getRedistemplate().opsForZSet();

                    matchSet.add("timeline_matchId", new MatchSession(matchId, this.platform), (double) System.currentTimeMillis() / 1000);
                }
                i++;
            }

            return timelineList.stream().map(CompletableFuture::join).toList();
        }

        public CompletableFuture<TimelineDto> getFuture(String matchId) {
            return get(matchId);
        }

    }

    public CompletableFuture<TimelineDto> byMatchIdFuture(String matchId) {
        return new TimeLine.Builder(matchId).platform(this.platform).getFuture(matchId);
    }

    public TimelineDto byMatchId(String matchId) {
        return new TimeLine.Builder(matchId).platform(this.platform).get();
    }

    public List<TimelineDto> byMatchIds(List<String> matchIds) {
        return new Builder(matchIds).platform(this.platform).getAll();
    }

}
