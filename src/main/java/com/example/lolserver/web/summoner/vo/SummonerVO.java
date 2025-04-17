package com.example.lolserver.web.summoner.vo;

import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import lombok.Getter;

import java.util.Set;

@Getter
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
}
