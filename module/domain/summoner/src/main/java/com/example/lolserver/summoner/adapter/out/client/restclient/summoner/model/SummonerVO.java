package com.example.lolserver.summoner.adapter.out.client.restclient.summoner.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
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
