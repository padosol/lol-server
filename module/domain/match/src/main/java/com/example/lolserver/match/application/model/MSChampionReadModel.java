package com.example.lolserver.match.application.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 다른 컨텍스트(duo 등)에 모스트 챔피언 요약을 노출하기 위한 읽기 모델.
 */
@Getter
@Builder
public class MSChampionReadModel {

    private int championId;
    private String championName;
    private Long playCount;
    private Long win;
    private Long losses;

    public static MSChampionReadModel of(MSChampionDetailReadModel champion) {
        return MSChampionReadModel.builder()
                .championId(champion.getChampionId())
                .championName(champion.getChampionName())
                .playCount(champion.getPlayCount())
                .win(champion.getWin())
                .losses(champion.getLosses())
                .build();
    }
}
