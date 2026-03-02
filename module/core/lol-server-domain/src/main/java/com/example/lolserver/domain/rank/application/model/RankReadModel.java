package com.example.lolserver.domain.rank.application.model;

import com.example.lolserver.domain.rank.domain.Rank;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class RankReadModel {

    private String puuid;
    private int currentRank;
    private int rankChange;
    private String gameName;
    private String tagLine;
    private int wins;
    private int losses;
    private BigDecimal winRate;
    private String tier;
    private String rank;
    private int leaguePoints;
    private List<String> champions;

    public RankReadModel(Rank rank) {
        this.puuid = rank.getPuuid();
        this.currentRank = rank.getCurrentRank();
        this.rankChange = rank.getRankChange();
        this.gameName = rank.getGameName();
        this.tagLine = rank.getTagLine();
        this.wins = rank.getWins();
        this.losses = rank.getLosses();
        this.winRate = rank.getWinRate();
        this.tier = rank.getTier();
        this.rank = rank.getRank();
        this.leaguePoints = rank.getLeaguePoints();
        this.champions = rank.getChampions();
    }
}
