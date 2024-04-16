package com.example.lolserver.riot.api.core.spectator;

import com.example.lolserver.riot.api.calling.RiotExecute;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.spectator.CurrentGameInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;

@Slf4j
public class SpectatorBuilder {

    private Platform platform;
    private String path;

    public SpectatorBuilder withPlatform(Platform platform) {
        this.platform = platform;
        return this;
    }

    public SpectatorBuilder withPuuid(String puuid) throws UnsupportedEncodingException {
        this.path = "/lol/spectator/v5/active-games/by-summoner/" + puuid;
        return this;
    }

    public CurrentGameInfo get() throws IOException, InterruptedException {

        RiotExecute execute = RiotExecute.getInstance();

        URI uri = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(this.platform.getRegion() + ".api.riotgames.com")
                .path(this.path)
                .build().toUri();

        CurrentGameInfo currentGameInfo = execute.execute(CurrentGameInfo.class, uri);
        log.info("currentGameInfo: {}", currentGameInfo);

        return currentGameInfo;
    }

}
