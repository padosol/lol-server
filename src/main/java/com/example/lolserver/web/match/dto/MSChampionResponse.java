package com.example.lolserver.web.match.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MSChampionResponse {

    private int championId;
    private String championName;
    private long kills;
    private long deaths;
    private long assists;
    private int playCount;


    public void kdaCalculator() {
    }

}
