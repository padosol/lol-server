package com.example.lolserver.web.summoner.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class SummonerResponse {

    private String summonerId;
    private String accountId;
    private String name;
    private int profileIconId;
    private String puuid;
//    private long revisionDate;
    private long summonerLevel;
    private String gameName;
    private String tagLine;
    private LocalDateTime lastRevisionDateTime;

}
