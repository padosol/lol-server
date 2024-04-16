package com.example.lolserver.riot.api.core.match;

import com.example.lolserver.riot.api.calling.RiotExecute;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class MatchBuilder {

    private Platform platform;
    private String path;

    public MatchBuilder withPlatform(Platform platform) {
        this.platform = platform;
        return this;
    }

    public MatchBuilder withMatchId(String matchId) throws UnsupportedEncodingException {
        this.path = MatchPath.MATCH.pathParam(matchId);
        return this;
    }

    public MatchBuilder withTimeline(String matchId) throws UnsupportedEncodingException {
        this.path = MatchPath.TIMELINE.pathParam(matchId);
        return this;
    }
    public MatchDto get() throws IOException, InterruptedException {

        RiotExecute execute = RiotExecute.getInstance();

        URI uri = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(this.platform.getCountry() + ".api.riotgames.com")
                .path(this.path)
                .build().toUri();

        MatchDto matchDto = execute.execute(MatchDto.class, uri);

        return matchDto;
    }

    public List<MatchDto> getAllMatches(Platform platform, List<String> matchIds) throws IOException, InterruptedException {
        this.platform = platform;

        List<MatchDto> matchDtoList = new ArrayList<>();

        for (String matchId : matchIds) {

            this.path = MatchPath.MATCH.pathParam(matchId);
            MatchDto matchDto = get();
            matchDtoList.add(matchDto);
        }

        return matchDtoList;
    }

}
