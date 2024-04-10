package com.example.lolserver.riot.api.core.summoner;

import com.example.lolserver.riot.api.calling.RiotExecute;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import org.springframework.web.util.UriComponentsBuilder;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SummonerBuilder {

    private Platform platform;
    private String path;


    public SummonerBuilder withPlatform(Platform platform) {
        this.platform = platform;
        return this;
    }

    public SummonerBuilder withName(String name) {
        this.path = SummonerPath.BY_NAME.pathParam(name);
        return this;
    }

    public SummonerBuilder withAccount(String account) {
        this.path = SummonerPath.BY_ACCOUNT.pathParam(account);
        return this;
    }

    public SummonerBuilder withPuuid(String puuid) {
        this.path = SummonerPath.BY_PUUID.pathParam(puuid);
        return this;
    }

    public SummonerDTO get() throws IOException, InterruptedException {
        RiotExecute execute = RiotExecute.getInstance();

        SummonerDTO summonerDTO = execute.execute(SummonerDTO.class, URI.create("https://"+this.platform.getRegion()+".api.riotgames.com" + this.path));

        return summonerDTO;
    }

}
