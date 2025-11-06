package com.example.lolserver.domain.rank.dto;

import com.example.lolserver.storage.redis.model.SummonerRankSession;
import lombok.Getter;

import java.util.List;

@Getter
public class RankResponse {

    private String summonerName;
    private String tagLine;
    private int win;
    private int losses;
    private int point;
    private String tier;
    private long summonerLevel;
    private String position;

    private List<String> championNames;

    public RankResponse(SummonerRankSession session) {
        this.summonerName = session.getSummonerName();
        this.tagLine = session.getTagLine();
        this.win = session.getWin();
        this.losses = session.getLosses();
        this.point = session.getPoint();
        this.tier = session.getTier().name() + " " + session.getDivision().name();
        this.summonerLevel = session.getSummonerLevel();
        this.position = session.getPosition();
        this.championNames = session.getChampionNames();

    }



}
