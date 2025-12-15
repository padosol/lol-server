package com.example.lolserver.domain.league.domain.vo;

import com.example.lolserver.repository.league.entity.LeagueSummonerHistoryEntity;

import java.time.LocalDateTime;

public record LeagueHistory(
    Long leagueSummonerId,
    String puuid,
    String queue,
    String leagueId,
    int wins,
    int losses,
    String tier,
    String rank,
    int leaguePoints,
    long absolutePoints,
    boolean veteran,
    boolean inactive,
    boolean freshBlood,
    boolean hotStreak,
    LocalDateTime createdAt
) {
    public LeagueHistory(LeagueSummonerHistoryEntity leagueSummonerHistory) {
        this(
                leagueSummonerHistory.getLeagueSummonerId(),
                leagueSummonerHistory.getPuuid(),
                leagueSummonerHistory.getQueue(),
                leagueSummonerHistory.getLeagueId(),
                leagueSummonerHistory.getWins(),
                leagueSummonerHistory.getLosses(),
                leagueSummonerHistory.getTier(),
                leagueSummonerHistory.getRank(),
                leagueSummonerHistory.getLeaguePoints(),
                leagueSummonerHistory.getAbsolutePoints(),
                leagueSummonerHistory.isVeteran(),
                leagueSummonerHistory.isInactive(),
                leagueSummonerHistory.isFreshBlood(),
                leagueSummonerHistory.isHotStreak(),
                leagueSummonerHistory.getCreatedAt()
        );
    }
}
