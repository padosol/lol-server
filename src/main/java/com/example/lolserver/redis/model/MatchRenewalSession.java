package com.example.lolserver.redis.model;

import java.util.List;

import lombok.Getter;

@Getter
public class MatchRenewalSession {

    private List<String> matchIds;
    private String puuid;

    public MatchRenewalSession(){};
    public MatchRenewalSession(String puuid, List<String> matchIds) {
        this.puuid = puuid;
        this.matchIds = matchIds;
    }
}
