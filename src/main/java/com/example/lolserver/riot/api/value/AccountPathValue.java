package com.example.lolserver.riot.api.value;

public enum AccountPathValue implements PathValue{
    PUUID("/riot/account/v1/accounts/by-puuid"),
    RIOT_ID("/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}"),
    ACTIVE_SHARDS("/riot/account/v1/active-shards/by-game/{game}/by-puuid/{puuid}"),
    ;

    public final String path;


    AccountPathValue(String path) {
        this.path = path;
    }
}
