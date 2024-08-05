package com.example.lolserver.web.summoner.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class SummonerResponse {

    private String summonerId;
    private String accountId;
    private int profileIconId;
    private String puuid;
    private long summonerLevel;
    private String gameName;
    private String tagLine;
    private String platform;
    private String lastRevisionDateTime;

    private String tier;
    private boolean notFound;

}
