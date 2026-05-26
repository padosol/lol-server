package com.example.lolserver.summoner.adapter.in.web.mapper;

import com.example.lolserver.QueueType;
import com.example.lolserver.summoner.adapter.in.web.response.LeagueResponse;
import com.example.lolserver.summoner.adapter.in.web.response.LeagueSummonerResponse;
import com.example.lolserver.summoner.domain.League;
import com.example.lolserver.summoner.domain.vo.LeagueHistory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class LeagueMapper {
    private LeagueMapper() {
    }

    private static final BigDecimal ONE_HUNDRED = new BigDecimal(100);

    public static LeagueResponse domainToResponse(List<League> leagues) {
        LeagueSummonerResponse soloLeague = null;
        LeagueSummonerResponse flexLeague = null;

        List<LeagueSummonerResponse> soloLeagueHistory = new ArrayList<>();
        List<LeagueSummonerResponse> flexLeagueHistory = new ArrayList<>();
        for (League league : leagues) {
            LeagueSummonerResponse leagueSummonerResponse = new LeagueSummonerResponse(
                    league.getQueue(),
                    league.getLeaguePoints(),
                    league.getWins(),
                    league.getLosses(),
                    league.getWinRate().multiply(ONE_HUNDRED).toString(),
                    league.getTier(),
                    league.getRank()
            );

            if (league.getQueue().equals(QueueType.RANKED_SOLO_5x5.name())) {
                soloLeague = leagueSummonerResponse;
            } else {
                flexLeague = leagueSummonerResponse;
            }

            List<LeagueHistory> leagueHistory = league.getLeagueHistory();
            for (LeagueHistory history : leagueHistory) {
                LeagueSummonerResponse leagueSummonerResponseHistory = new LeagueSummonerResponse(
                        history.queue(),
                        history.leaguePoints(),
                        history.wins(),
                        history.losses(),
                        calculateWinRate(history.wins(), history.losses()).toString(),
                        history.tier(),
                        history.rank()
                );

                if (history.queue().equals(QueueType.RANKED_SOLO_5x5.name())) {
                    soloLeagueHistory.add(leagueSummonerResponseHistory);
                } else {
                    flexLeagueHistory.add(leagueSummonerResponseHistory);
                }
            }
        }

        return new LeagueResponse(
                soloLeague, flexLeague, soloLeagueHistory, flexLeagueHistory
        );
    }

    private static BigDecimal calculateWinRate(int wins, int losses) {
        BigDecimal winGames = BigDecimal.valueOf(wins);
        BigDecimal lossesGames = BigDecimal.valueOf(losses);
        BigDecimal totalGames = winGames.add(lossesGames);

        return winGames.divide(totalGames, 2, RoundingMode.HALF_UP);
    }
}
