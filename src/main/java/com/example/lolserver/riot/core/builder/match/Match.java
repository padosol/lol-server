package com.example.lolserver.riot.core.builder.match;

import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.type.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
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

        private CompletableFuture<MatchDto> get(String matchId) {

            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            builder.scheme("https").host(this.platform.getPlatform() + ".api.riotgames.com");
            builder.path("/lol/match/v5/matches/" + matchId);

            return RiotAPI.getExecute().execute(MatchDto.class, builder.build().toUri());
        }

        private List<MatchDto> getAll()  {

            List<CompletableFuture<MatchDto>> matchList = new ArrayList<>();

            for(String matchId : this.matchIds) {
                CompletableFuture<MatchDto> future = get(matchId);
                matchList.add(future);
            }

            CompletableFuture<List<MatchDto>> allMatchDto = CompletableFuture.allOf(matchList.toArray(new CompletableFuture[matchList.size()]))
                    .thenApply( v -> matchList.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList()));

            try {
                return allMatchDto.get();
            } catch(ExecutionException | InterruptedException e) {
                return null;
            }
        }

    }

    public MatchDto byMatchId(String matchId) {
        return new Builder(matchId).platform(this.platform).get();
    }

    public List<MatchDto> byMatchIds(List<String> matchIds) {
        return new Builder(matchIds).platform(this.platform).getAll();
    }

}
