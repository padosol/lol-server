package com.example.lolserver.riot.api.core.champion;

import com.example.lolserver.riot.api.calling.RiotExecute;
import com.example.lolserver.riot.api.core.match.MatchBuilder;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.champion.ChampionInfo;
import com.example.lolserver.riot.dto.match.MatchDto;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

public class ChampionBuilder {

    private Platform platform;

    public ChampionBuilder withPlatform(Platform platform) {
        this.platform = platform;
        return this;
    }

    public ChampionInfo get() throws IOException, InterruptedException {
        RiotExecute execute = RiotExecute.getInstance();

        URI uri = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(this.platform.getCountry() + ".api.riotgames.com")
                .path("/lol/platform/v3/champion-rotations")
                .build().toUri();

        ChampionInfo result = execute.execute(ChampionInfo.class, uri);

        return result;
    }

}
