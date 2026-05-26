package com.example.lolserver.match.application.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 특정 puuid가 한 경기에서 사용한 챔피언과 승패를 나타내는 읽기 모델.
 *
 * <p>다른 컨텍스트(duo 등)가 최근 전적 요약을 계산할 때 사용한다.
 */
@Getter
@Builder
public class PlayerMatchReadModel {

    private boolean win;
    private int championId;
    private String championName;
}
