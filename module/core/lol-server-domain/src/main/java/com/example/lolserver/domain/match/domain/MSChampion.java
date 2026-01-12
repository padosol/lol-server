package com.example.lolserver.domain.match.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MSChampion {
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
}
