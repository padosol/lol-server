package com.example.lolserver.rabbitmq.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class SummonerMessage {
    private String platform;
    private String puuid;
    private long revisionDate;

    public SummonerMessage(){}

    public SummonerMessage(String platform, String puuid, Long revisionDate) {
        this.platform = platform;
        this.puuid = puuid;
        this.revisionDate = revisionDate;
    }

}
