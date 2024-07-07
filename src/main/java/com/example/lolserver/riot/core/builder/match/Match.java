package com.example.lolserver.riot.core.builder.match;

import com.example.lolserver.redis.model.MatchSession;
import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.core.calling.RiotExecuteProxy;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.type.Platform;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class Match {

    private Platform platform;

    public Match(Platform platform) {
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

        public Builder platform(Platform platform) {
            this.platform = platform;
            return this;
        }

        public MatchDto get()  {

            try {
                MatchDto matchDto = get(this.matchId).get();
                return matchDto;
            } catch(ExecutionException | InterruptedException e) {
                e.printStackTrace();
                log.info("API ERROR");
                return null;
            }

        }

        public CompletableFuture<MatchDto> getFuture(String matchId) {
            return get(matchId);
        }

        private CompletableFuture<MatchDto> get(String matchId) {

            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            builder.scheme("https").host(this.platform.getPlatform() + ".api.riotgames.com");
            builder.path("/lol/match/v5/matches/" + matchId);

            return RiotAPI.getExecute().execute(MatchDto.class, builder.build().toUri());
        }

        private List<MatchDto> getAll()  {

            List<CompletableFuture<MatchDto>> matchList = new ArrayList<>();

            Bucket bucket = RiotAPI.getBucket();

            int i = 0;
            for(String matchId : this.matchIds) {

                if( i > 20 ) {
                    ZSetOperations<String, Object> matchSet = RiotAPI.getRedistemplate().opsForZSet();

                    matchSet.add("matchId", new MatchSession(matchId, this.platform), (double) System.currentTimeMillis() / 1000);
                }

                if(bucket.getAvailableTokens() > 0) {
                    CompletableFuture<MatchDto> future = get(matchId);
                    matchList.add(future);
                } else {
                    ZSetOperations<String, Object> matchSet = RiotAPI.getRedistemplate().opsForZSet();

                    matchSet.add("matchId", new MatchSession(matchId, this.platform), (double) System.currentTimeMillis() / 1000);
                }
                i++;
            }

            return matchList.stream().map(CompletableFuture::join).toList();
        }

    }
    public CompletableFuture<MatchDto> byMatchIdFuture(String matchId) {
        return new Builder(matchId).platform(this.platform).getFuture(matchId);
    }


    public MatchDto byMatchId(String matchId) {
        return new Builder(matchId).platform(this.platform).get();
    }

    public List<MatchDto> byMatchIds(List<String> matchIds) {
        return new Builder(matchIds).platform(this.platform).getAll();
    }

}
