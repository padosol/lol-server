package com.example.lolserver.web.dto.data;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SummonerData {

    private String summonerId;
    private String accountId;
    private String name;
    private int profileIconId;
    private String puuid;
    private long revisionDate;
    private long summonerLevel;
    private String gameName;
    private String tagLine;


}
