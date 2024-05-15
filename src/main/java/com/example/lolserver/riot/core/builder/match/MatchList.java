package com.example.lolserver.riot.core.builder.match;

import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.type.Platform;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class MatchList {

    private Platform platform;

    public MatchList(Platform platform) {
        this.platform = platform;
    }

    @Getter
    @Setter
    @lombok.Builder
    public static class MatchQuery {
        private Long startTime;
        private Long endTime;
        private Integer queue;
        private String type;

        @lombok.Builder.Default
        private Integer start = 0;

        @lombok.Builder.Default
        private Integer count = 20;

        public MultiValueMap<String, String> getParams() {

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

            if(this.startTime != null) {
                params.add("startTime", this.startTime.toString());
            }

            if(this.endTime != null) {
                params.add("endTime", this.endTime.toString());
            }

            if(this.queue != null) {
                params.add("queue", this.queue.toString());
            }

            if(this.type != null) {
                params.add("type", this.type);
            }

            if(this.start != null) {
                params.add("start", this.start.toString());
            }

            if(this.count != null) {
                params.add("count", this.count.toString());
            }
            return params;
        }
    }

    public static class Builder {

        private Platform platform;
        private String puuid;
        private MultiValueMap<String, String> query;

        public Builder(String puuid) {
            this.puuid = puuid;
        }

        public Builder query(Function<MatchQuery.MatchQueryBuilder, MatchQuery> queryBuilder) {
            MatchQuery query = queryBuilder.apply(MatchQuery.builder());
            this.query = query.getParams();
            return this;
        }

        public Builder platform(Platform platform) {
            this.platform = platform;
            return this;
        }

        public List<String> get() {

            UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host(platform.getPlatform() + ".api.riotgames.com")
                    .path("/lol/match/v5/matches/by-puuid/" + this.puuid + "/ids");

            if(this.query != null) {
                builder.queryParams(this.query);
            } else {
                builder.queryParams(MatchQuery.builder().build().getParams());
            }

            try {
                String[] result = RiotAPI.getExecute().execute(String[].class, builder.build().toUri()).get();

                return Arrays.stream(result).toList();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public Builder byPuuid(String puuid) {
        return new Builder(puuid).platform(platform);
    }

}
