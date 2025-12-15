package com.example.lolserver.domain.match.domain;

import com.example.lolserver.repository.match.dto.MSChampionDTO;
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

    public static MSChampion of(MSChampionDTO msChampionDTO) {
        return new MSChampion(
                msChampionDTO.getAssists(),
                msChampionDTO.getDeaths(),
                msChampionDTO.getKills(),
                msChampionDTO.getChampionId(),
                msChampionDTO.getChampionName(),
                msChampionDTO.getWin(),
                msChampionDTO.getLosses(),
                msChampionDTO.getWinRate(),
                msChampionDTO.getDamagePerMinute(),
                msChampionDTO.getKda(),
                msChampionDTO.getLaneMinionsFirst10Minutes(),
                msChampionDTO.getDamageTakenOnTeamPercentage(),
                msChampionDTO.getGoldPerMinute(),
                msChampionDTO.getPlayCount()
        );
    }
}
