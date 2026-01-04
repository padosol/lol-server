package com.example.lolserver.domain.rank.application.dto;

import com.example.lolserver.domain.rank.domain.Rank;
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

    public RankResponse(Rank rank) {
        this.summonerName = rank.getSummonerName();
        this.tagLine = rank.getTagLine();
        this.win = rank.getWin();
        this.losses = rank.getLosses();
        this.point = rank.getPoint();
        this.tier = rank.getTier();
        this.summonerLevel = rank.getSummonerLevel();
        this.position = rank.getPosition();
        this.champions = rank.getChampions().stream().map(
                championName -> Map.of(
                        "championName", championName,
                        "championImgUrl", "https://opgg-static.akamaized.net/champion/" + championName + ".png"
                )
        ).collect(Collectors.toList());
    }
}
