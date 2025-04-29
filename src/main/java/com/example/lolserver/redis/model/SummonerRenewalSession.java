package com.example.lolserver.redis.model;


import java.io.Serializable;

import org.springframework.data.redis.core.RedisHash;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RedisHash(value = "summonerRenewal", timeToLive = 120)
public class SummonerRenewalSession implements Serializable {

    @Id
    private String id;

    private boolean accountUpdate;
    private boolean summonerUpdate;
    private boolean leagueUpdate;
    private boolean matchUpdate;

    public SummonerRenewalSession() {};
    public SummonerRenewalSession(String puuid) {
        this.id = puuid;
        this.summonerUpdate = false;
        this.leagueUpdate = false;
        this.matchUpdate = false;
    }

    public void allUpdate(){
        this.accountUpdate = true;
        this.summonerUpdate = true;
        this.leagueUpdate = true;
        this.matchUpdate = true;
    }

    public boolean checkAllUpdated() {
        return this.summonerUpdate && this.accountUpdate && this.leagueUpdate && this.matchUpdate;
    }

}
