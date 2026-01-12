package com.example.lolserver.restclient.summoner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SummonerVO {
    private String puuid;
    private String gameName;
    private String tagLine;
    private int profileIconId;
    private LocalDateTime revisionDate;
    private long summonerLevel;

    private Set<LeagueEntryDTO> leagueEntryDTOS;

    public SummonerVO() {}
}
