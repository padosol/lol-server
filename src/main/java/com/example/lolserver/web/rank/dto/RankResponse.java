package com.example.lolserver.web.rank.dto;

import com.example.lolserver.redis.model.SummonerRankSession;
import lombok.Getter;

@Getter
public class RankResponse {

    private String summonerName;
    private int win;
    private int losses;
    private int point;
    private String tier;
    public RankResponse(SummonerRankSession session) {
        this.summonerName = session.getSummonerName();
        this.win = session.getWin();
        this.losses = session.getLosses();
        this.point = session.getPoint();
        this.tier = session.getTier().name() + " " + session.getDivision().name();
    }

}
