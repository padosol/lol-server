package com.example.lolserver.repository.dto.data.leagueData;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeagueSummonerData {

    private String leagueType;
    private int leaguePoints;
    private int wins;
    private int losses;
    private String oow;
    private String leagueImage;
    private String tier;
    private String rank;
}