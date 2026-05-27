package com.example.lolserver.match.application.model.readmodel;

import lombok.Builder;
import lombok.Getter;

/**
 * 랭크 챔피언 통계 상세 읽기 모델 (rank/champions API 용 풀 통계).
 * 영속 어댑터가 MapStruct(MSChampionDTO→)로 직접 빌드한다.
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
}
