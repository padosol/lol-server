package com.example.lolserver.service;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@ToString
public class SummonerMessage {
    private String platformId;
    private String puuid;
    private long revisionDate;

    public SummonerMessage(String platformId, String puuid, LocalDateTime revisionDate) {
        this.platformId = platformId;
        this.puuid = puuid;

        this.revisionDate = revisionDate.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

}
