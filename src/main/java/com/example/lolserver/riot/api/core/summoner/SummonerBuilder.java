package com.example.lolserver.riot.api.core.summoner;

import com.example.lolserver.riot.api.calling.RiotExecute;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;

public class SummonerBuilder {

    private Platform platform;
    private String path;
    private String query;


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

    public SummonerDTO get() {
        RiotExecute execute = new RiotExecute();



        return null;
    }

}
