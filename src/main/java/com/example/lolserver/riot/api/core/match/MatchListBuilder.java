package com.example.lolserver.riot.api.core.match;

import com.example.lolserver.riot.MatchParameters;
import com.example.lolserver.riot.api.RiotApi;
import com.example.lolserver.riot.api.calling.RiotExecute;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.match.MatchDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class MatchListBuilder {

    private Platform platform;
    private String path;
    private MatchQuery query;


    @Getter
    @Setter
    @Builder
    public static class MatchQuery {
        private Long startTime;
        private Long endTime;
        private Integer queue;
        private String type;

        @Builder.Default
        private Integer start = 0;

        @Builder.Default
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

    public MatchListBuilder withPlatform(Platform platform) {
        this.platform = platform;
        return this;
    }

    public MatchListBuilder withPuuid(String puuid) throws UnsupportedEncodingException {
        this.path = MatchPath.BY_PUUID.pathParam(puuid);
        return this;
    }

    public MatchListBuilder queryParam(Function<MatchQuery.MatchQueryBuilder, MatchQuery> queryBuilder) {
        this.query = queryBuilder.apply(MatchQuery.builder());
        return this;
    }


    public List<String> get() throws IOException, InterruptedException {

        if(this.query == null) {
            this.query = MatchQuery.builder().build();
        }

        RiotExecute execute = RiotExecute.getInstance();

        URI uri = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(this.platform.getCountry() + ".api.riotgames.com")
                .path(this.path)
                .queryParams(this.query.getParams())
                .build().toUri();

        String[] result = execute.execute(String[].class, uri);

        List<String> matchIds = Arrays.stream(result).toList();

        return matchIds;
    }

    public List<String> getAll() throws IOException, InterruptedException {

        List<String> matchList = new ArrayList<>();

        boolean flag = true;
        long endTime = Instant.now().getEpochSecond();

        if(this.query == null) {
            this.query = MatchQuery.builder().build();
        }

        this.query.setStartTime(RiotApi.START_TIME);
        this.query.setCount(100);
        this.query.setStart(0);

        MatchBuilder matchBuilder = new MatchBuilder();

        while(flag) {

            List<String> matchIds = get();
            matchList.addAll(matchIds);

            if(matchIds.size() == 100) {
                MatchDto matchDto = matchBuilder.withPlatform(this.platform).withMatchId(matchIds.get(99)).get();
                endTime = matchDto.getInfo().getGameCreation() / 1000;
                this.query.setEndTime(endTime);
            } else {
                flag = false;
            }

        }

        return matchList;
    }

//    public List<String> getAll() throws IOException, InterruptedException {
//
//        List<String> matchList = new ArrayList<>();
//
//        boolean flag = true;
//        long endTime = Instant.now().getEpochSecond();
//
//        if(this.query == null) {
//            this.query = MatchQuery.builder().build();
//        }
//
//        this.query.setStartTime(RiotApi.START_TIME);
//        this.query.setCount(20);
//        this.query.setStart(0);
//
//        List<String> matchIds = get();
//        matchList.addAll(matchIds);
//
//        return matchList;
//    }

}
