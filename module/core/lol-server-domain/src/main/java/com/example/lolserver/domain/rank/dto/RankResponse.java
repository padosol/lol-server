package com.example.lolserver.domain.rank.dto;

import com.example.lolserver.model.SummonerRankSession;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private List<Map<String,String>> champions;

    public RankResponse(SummonerRankSession session) {
        this.summonerName = session.getSummonerName();
        this.tagLine = session.getTagLine();
        this.win = session.getWin();
        this.losses = session.getLosses();
        this.point = session.getPoint();
        this.tier = session.getTier().name() + " " + session.getDivision().name();
        this.summonerLevel = session.getSummonerLevel();
        this.position = session.getPosition();
        this.champions = session.getChampionNames().stream().map(
                championName -> Map.of(
                        "championName", championName,
                        "championImgUrl", "https://opgg-static.akamaized.net/champion/" + championName + ".png"
                )
        ).collect(Collectors.toList());

    }



}
