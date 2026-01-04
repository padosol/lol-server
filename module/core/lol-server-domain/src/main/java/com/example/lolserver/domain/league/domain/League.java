package com.example.lolserver.domain.league.domain;

import com.example.lolserver.domain.league.domain.vo.LeagueHistory;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Getter
@Builder
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

    @Builder.Default
    private List<LeagueHistory> leagueHistory = new ArrayList<>();

    public BigDecimal calculateWinRate(int wins, int losses) {
        BigDecimal winGames = BigDecimal.valueOf(wins);
        BigDecimal lossesGames = BigDecimal.valueOf(losses);
        BigDecimal totalGames = winGames.add(lossesGames);

        if (totalGames.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return winGames.divide(totalGames, 2, RoundingMode.HALF_UP);
    }

    public void addAllHistoryDomain(List<LeagueHistory> leagueHistory) {
        this.leagueHistory.addAll(leagueHistory);
    }
}
