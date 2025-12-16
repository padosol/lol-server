package com.example.lolserver.service;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@ToString
public class SummonerMessage {
    private String platform;
    private String puuid;
    private long revisionDate;

    public SummonerMessage(String platform, String puuid, LocalDateTime revisionDate) {
        this.platform = platform;
        this.puuid = puuid;

        this.revisionDate = revisionDate.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

}
