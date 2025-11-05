package com.example.lolserver.storage.db.core.repository.match.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MSChampionResponse {

    private int championId;
    private String championName;
    private Double kills;
    private Double deaths;
    private Double assists;
    private Double cs;
    private Double duration;
    private Long win;
    private Long playCount;

}
