package com.example.lolserver.riot.api;

import com.example.lolserver.riot.api.core.match.MatchAPI;
import com.example.lolserver.riot.api.core.summoner.SummonerAPI;

public class RiotApi {

    public static SummonerAPI summoner() {
        return SummonerAPI.getInstance();
    }

    public static MatchAPI match() {return MatchAPI.getInstance();}

}
