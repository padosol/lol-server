package com.example.lolserver.riot.dto.summoner;

import com.example.lolserver.riot.dto.error.ErrorDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SummonerDTO extends ErrorDTO {

    private String accountId;
    private int profileIconId;
    private long revisionDate;
    private String id;
    private String puuid;
    private long summonerLevel;

}