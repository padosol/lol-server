package com.example.lolserver.match.application.model;

import com.example.lolserver.match.domain.MSChampion;
import lombok.Builder;
import lombok.Getter;

/**
 * 랭크 챔피언 통계 상세 읽기 모델 (rank/champions API 용 풀 통계).
 * 다른 컨텍스트 노출용 요약 모델은 {@link MSChampionReadModel} 참고.
 */
@Getter
@Builder
public class MSChampionDetailReadModel {

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

    public static MSChampionDetailReadModel of(MSChampion champion) {
        return MSChampionDetailReadModel.builder()
                .assists(champion.getAssists())
                .deaths(champion.getDeaths())
                .kills(champion.getKills())
                .championId(champion.getChampionId())
                .championName(champion.getChampionName())
                .win(champion.getWin())
                .losses(champion.getLosses())
                .winRate(champion.getWinRate())
                .damagePerMinute(champion.getDamagePerMinute())
                .kda(champion.getKda())
                .laneMinionsFirst10Minutes(champion.getLaneMinionsFirst10Minutes())
                .damageTakenOnTeamPercentage(champion.getDamageTakenOnTeamPercentage())
                .goldPerMinute(champion.getGoldPerMinute())
                .playCount(champion.getPlayCount())
                .build();
    }
}
