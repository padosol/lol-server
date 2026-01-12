package com.example.lolserver.domain.rank.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class Rank {
    private String summonerName;
    private String tagLine;
    private int win;
    private int losses;
    private int point;
    private String tier;
    private long summonerLevel;
    private String position;
    private List<String> champions;
}
