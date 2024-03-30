package com.example.lolserver.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SummonerResponse {

    private String gameName;
    private String name;
    private Integer profileIconId;
    private String shard;
    private String summonerId;
    private Integer summonerLevel;
    private String tagLine;

}
