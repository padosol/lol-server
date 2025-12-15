package com.example.lolserver.domain.league.domain;

import com.example.lolserver.domain.league.domain.vo.LeagueHistory;
import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummoner;
import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummonerHistory;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class League {
    private Long id;
    private String leagueId;
    private String puuid;
    private String queue;
    private int wins;
    private int losses;
    private BigDecimal winRate;
    private String tier;
    private String rank;
    private int leaguePoints;
    private boolean veteran;
    private boolean inactive;
    private boolean freshBlood;
    private boolean hotStreak;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;

    private List<LeagueHistory>  leagueHistory = new ArrayList<>();

    public League(LeagueSummoner leagueSummoner) {
        this.id = leagueSummoner.getId();
        this.leagueId = leagueSummoner.getLeagueId();
        this.puuid = leagueSummoner.getPuuid();
        this.queue = leagueSummoner.getQueue();
        this.wins = leagueSummoner.getWins();
        this.losses = leagueSummoner.getLosses();
        this.tier = leagueSummoner.getTier();
        this.rank = leagueSummoner.getRank();
        this.leaguePoints = leagueSummoner.getLeaguePoints();
        this.veteran = leagueSummoner.isVeteran();
        this.inactive = leagueSummoner.isInactive();
        this.freshBlood = leagueSummoner.isFreshBlood();
        this.hotStreak = leagueSummoner.isHotStreak();
        this.createAt = leagueSummoner.getCreateAt();
        this.updateAt = leagueSummoner.getUpdateAt();

        this.winRate = calculateWinRate(this.wins, this.losses);
    }

    public BigDecimal calculateWinRate(int wins, int losses) {
        BigDecimal winGames = BigDecimal.valueOf(wins);
        BigDecimal lossesGames = BigDecimal.valueOf(losses);
        BigDecimal totalGames = winGames.add(lossesGames);

        return winGames.divide(totalGames, 2, RoundingMode.HALF_UP);
    }

    public void addAllHistory(List<LeagueSummonerHistory> leagueHistory) {
        List<LeagueHistory> leagueHistories = leagueHistory.stream().map(LeagueHistory::new).toList();
        this.leagueHistory.addAll(leagueHistories);
    }

}
