package com.example.lolserver.storage.db.core.repository.summoner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummonerResponse {
    private int profileIconId;
    private String puuid;
    private long summonerLevel;
    private String gameName;
    private String tagLine;
    private String platform;
    private String lastRevisionDateTime;
    private Integer point;
    private String tier;
    private String rank;
}
