package com.example.lolserver.summoner.application.model;

import com.example.lolserver.summoner.domain.League;
import lombok.Builder;
import lombok.Getter;

/**
 * 다른 컨텍스트(duo 등)에 리그(티어) 정보를 노출하기 위한 읽기 모델.
 */
@Getter
@Builder
public class LeagueReadModel {

    private String queue;
    private String tier;
    private String rank;
    private int leaguePoints;

    public static LeagueReadModel of(League league) {
        return LeagueReadModel.builder()
                .queue(league.getQueue())
                .tier(league.getTier())
                .rank(league.getRank())
                .leaguePoints(league.getLeaguePoints())
                .build();
    }
}
