package com.example.lolserver.domain.rank.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class Rank {
    private String puuid;
    private int currentRank;
    private int rankChange;
    private String gameName;
    private String tagLine;
    private int wins;
    private int losses;
    private BigDecimal winRate;
    private String tier;
    private int leaguePoints;
    private List<String> champions;
}
