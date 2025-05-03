package com.example.lolserver.redis.model;


import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RedisHash(value = "summonerRenewal", timeToLive = 10)
public class SummonerRenewalSession implements Serializable {

    @Id
    private String puuid;

    private boolean summonerUpdate;
    private boolean leagueUpdate;
    private boolean matchUpdate;

    public SummonerRenewalSession() {};
    public SummonerRenewalSession(String puuid) {
        this.puuid = puuid;
        this.summonerUpdate = false;
        this.leagueUpdate = false;
        this.matchUpdate = false;
    }
}
