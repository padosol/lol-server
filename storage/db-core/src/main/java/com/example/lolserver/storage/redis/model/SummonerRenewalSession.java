package com.example.lolserver.storage.redis.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Getter
@Setter
@RedisHash(value = "summonerRenewal", timeToLive = 120)
@NoArgsConstructor
@AllArgsConstructor
public class SummonerRenewalSession implements Serializable {

    @Id
    private String puuid;

    private boolean summonerUpdate;
    private boolean leagueUpdate;
    private boolean matchUpdate;

    public SummonerRenewalSession(String puuid) {
        this.puuid = puuid;
        this.summonerUpdate = false;
        this.leagueUpdate = false;
        this.matchUpdate = false;
    }

}
