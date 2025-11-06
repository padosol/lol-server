package com.example.lolserver.domain.match.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MSChampionRequest {

    private String puuid;
    private Integer season;
    private Integer queueId;
    private String platform;

}
