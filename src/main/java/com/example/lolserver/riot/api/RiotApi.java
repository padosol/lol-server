package com.example.lolserver.riot.api;

import com.example.lolserver.riot.api.core.champion.ChampionAPI;
import com.example.lolserver.riot.api.core.match.MatchAPI;
import com.example.lolserver.riot.api.core.spectator.SpectatorAPI;
import com.example.lolserver.riot.api.core.summoner.SummonerAPI;

import java.util.concurrent.ConcurrentHashMap;

public class RiotApi {
    public static final Long START_TIME = 1704855600L;

    private static final ConcurrentHashMap<String, Integer> SUMMONER_RENEWAL_LIST = new ConcurrentHashMap<>();

    public static int REQUEST_COUNT = 0;

    public static SummonerAPI summoner() {
        return SummonerAPI.getInstance();
    }

    public static MatchAPI match() {return MatchAPI.getInstance();}

    public static SpectatorAPI spectator() {return SpectatorAPI.getInstance();}

    public static ChampionAPI champion() {return ChampionAPI.getInstance();}

    public static ConcurrentHashMap<String, Integer> getRenewalList() {
        return SUMMONER_RENEWAL_LIST;
    }

}
