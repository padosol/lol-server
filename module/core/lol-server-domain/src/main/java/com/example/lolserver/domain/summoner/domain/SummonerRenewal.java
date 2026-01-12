package com.example.lolserver.domain.summoner.domain;

import com.example.lolserver.RenewalStatus;
import lombok.Getter;

@Getter
public class SummonerRenewal {
    private String puuid;
    private RenewalStatus status;

    public SummonerRenewal(String puuid,  RenewalStatus status) {
        this.puuid = puuid;
        this.status = status;
    }
}
