package com.example.lolserver.riot.api.core.summoner;

public enum SummonerPath {

    BY_NAME("/lol/summoner/v4/summoners/by-name/{name}", "{name}"),
    BY_PUUID("/lol/summoner/v4/summoners/by-puuid/{puuid}", "{puuid}"),
    BY_ACCOUNT("/lol/summoner/v4/summoners/by-account/{account}", "{account}"),
    ;

    final String path;
    final String key;

    SummonerPath(String path, String key){
        this.path = path;
        this.key = key;
    }

    public String pathParam(String param) {
        return this.path.replace(this.key, param);
    }


}
