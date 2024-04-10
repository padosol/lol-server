package com.example.lolserver.riot.api.core.match;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    public String pathParam(String param) throws UnsupportedEncodingException {
        return this.path.replace(this.key, param);
    }

}
