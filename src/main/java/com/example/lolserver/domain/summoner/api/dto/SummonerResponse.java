package com.example.lolserver.domain.summoner.api.dto;

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

    private Integer point;
    private String tier;
    private boolean notFound;

}
