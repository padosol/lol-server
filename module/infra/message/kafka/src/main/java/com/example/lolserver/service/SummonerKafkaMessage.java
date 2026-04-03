package com.example.lolserver.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@ToString
@NoArgsConstructor
public class SummonerKafkaMessage {

    private String platformId;
    private String puuid;
    private long revisionDate;

    public SummonerKafkaMessage(String platformId, String puuid, LocalDateTime revisionDate) {
        this.platformId = platformId;
        this.puuid = puuid;
        this.revisionDate = revisionDate.atZone(ZoneId.systemDefault()).toEpochSecond();
    }
}
