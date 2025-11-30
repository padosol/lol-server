package com.example.lolserver.storage.db.core.repository.match.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

@Getter
public class MSChampionDTO {

    private Double assists;
    private Double deaths;
    private Double kills;
    private int championId;
    private String championName;
    private Long win;
    private Long losses;
    private Double winRate;
    private Double damagePerMinute;
    private Double kda;
    private Double laneMinionsFirst10Minutes;
    private Double damageTakenOnTeamPercentage;
    private Double goldPerMinute;
    private Long playCount;

    @QueryProjection
    public MSChampionDTO(Double assists, Double deaths, Double kills, int championId, String championName, Long win, Long losses, Double damagePerMinute, Double kda, Double laneMinionsFirst10Minutes, Double damageTakenOnTeamPercentage, Double goldPerMinute, Long playCount) {
        this.assists = assists;
        this.deaths = deaths;
        this.kills = kills;
        this.championId = championId;
        this.championName = championName;
        this.win = win;
        this.losses = losses;
        this.damagePerMinute = damagePerMinute;
        this.kda = kda;
        this.laneMinionsFirst10Minutes = laneMinionsFirst10Minutes;
        this.damageTakenOnTeamPercentage = damageTakenOnTeamPercentage;
        this.goldPerMinute = goldPerMinute;
        this.playCount = playCount;
        this.winRate = (this.playCount != null && this.playCount > 0) ? (this.win * 100.0 / this.playCount) : 0.0;
    }
}
