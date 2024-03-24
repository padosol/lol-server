package com.example.lolserver.web.dto.data;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class SummonerData {

    private String summonerId;
    private String accountId;
    private String name;
    private int profileIconId;
    private String puuid;
//    private long revisionDate;
    private long summonerLevel;
    private String gameName;
    private String tagLine;
    private LocalDateTime lastRevisionDateTime;

}
