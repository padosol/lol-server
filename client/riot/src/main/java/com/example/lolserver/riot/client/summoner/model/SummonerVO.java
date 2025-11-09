package com.example.lolserver.riot.client.summoner.model;

import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.Set;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SummonerVO {
    private String puuid;
    private String gameName;
    private String tagLine;
    private String id;
    private String accountId;
    private int profileIconId;
    private long revisionDate;
    private long summonerLevel;

    private Set<LeagueEntryDTO> leagueEntryDTOS;

    public SummonerVO() {}
}
