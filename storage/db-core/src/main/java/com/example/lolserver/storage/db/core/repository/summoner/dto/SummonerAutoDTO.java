package com.example.lolserver.storage.db.core.repository.summoner.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

@Getter
public class SummonerAutoDTO {
    private String gameName;
    private String tagLine;
    private int profileIconId;
    private long summonerLevel;
    private String tier;
    private String rank;
    private int leaguePoints;

    @QueryProjection
    public SummonerAutoDTO(String gameName, String tagLine, int profileIconId, long summonerLevel, String tier, String rank, int leaguePoints) {
        this.gameName = gameName;
        this.tagLine = tagLine;
        this.profileIconId = profileIconId;
        this.summonerLevel = summonerLevel;
        this.tier = tier;
        this.rank = rank;
        this.leaguePoints = leaguePoints;
    }
}
