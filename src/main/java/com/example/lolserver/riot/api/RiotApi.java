package com.example.lolserver.riot.api;

import com.example.lolserver.riot.api.core.summoner.SummonerAPI;

public class RiotApi {

    public static SummonerAPI summoner() {
        return SummonerAPI.getInstance();
    }

}
