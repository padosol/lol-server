package com.example.lolserver.riot.api.core.match;

public enum MatchPath {

    BY_PUUID("/lol/match/v5/matches/by-puuid/{puuid}/ids", "{puuid}"),
    MATCH("/lol/match/v5/matches/{matchId}", "{matchId}"),
    TIMELINE("/lol/match/v5/matches/{matchId}/timeline", "{matchId}"),
    ;

    final String path;
    final String key;

    MatchPath(String path, String key){
        this.path = path;
        this.key = key;
    }

    public String pathParam(String param) {
        return this.path.replace(this.key, param);
    }

}
