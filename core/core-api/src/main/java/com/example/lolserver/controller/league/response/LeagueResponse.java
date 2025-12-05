package com.example.lolserver.controller.league.response;

import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummoner;
import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummonerHistory;
import com.example.lolserver.storage.db.core.repository.league.entity.QueueType;

import java.util.ArrayList;
import java.util.List;

public record LeagueResponse(
    LeagueSummonerResponse soloLeague,
    LeagueSummonerResponse flexLeague,

    List<LeagueSummonerResponse> soloLeagueHistory,
    List<LeagueSummonerResponse> flexLeagueHistory
) {

    public static LeagueResponse of(List<LeagueSummoner> leagueSummoners, List<LeagueSummonerHistory> leagueSummonerHistories) {
        LeagueSummonerResponse soloLeague = null;
        LeagueSummonerResponse flexLeague = null;

        List<LeagueSummonerResponse> solo = new ArrayList<>();
        List<LeagueSummonerResponse> flex = new ArrayList<>();

        for (LeagueSummoner leagueSummoner : leagueSummoners) {
            LeagueSummonerResponse leagueSummonerResponse = new LeagueSummonerResponse(
                    leagueSummoner.getQueue(),
                    leagueSummoner.getLeaguePoints(),
                    leagueSummoner.getWins(),
                    leagueSummoner.getLosses(),
                    ( String.format("%.2f",
                            (((double) leagueSummoner.getWins() / (leagueSummoner.getWins() + leagueSummoner.getLosses())))*100 ) + "%" ),
                    leagueSummoner.getTier(),
                    leagueSummoner.getRank()
            );

            if (leagueSummoner.getQueue().equals(QueueType.RANKED_SOLO_5x5.name())) {
                soloLeague = leagueSummonerResponse;
            } else {
                flexLeague= leagueSummonerResponse;
            }
        }

        for (LeagueSummonerHistory leagueSummonerHistory : leagueSummonerHistories) {
            LeagueSummonerResponse leagueSummonerResponse = new LeagueSummonerResponse(
                    leagueSummonerHistory.getQueue(),
                    leagueSummonerHistory.getLeaguePoints(),
                    leagueSummonerHistory.getWins(),
                    leagueSummonerHistory.getLosses(),
                    ( String.format("%.2f",
                            (((double) leagueSummonerHistory.getWins() / (leagueSummonerHistory.getWins() + leagueSummonerHistory.getLosses())))*100 ) + "%" ),
                    leagueSummonerHistory.getTier(),
                    leagueSummonerHistory.getRank()
            );

            if (leagueSummonerHistory.getQueue().equals(QueueType.RANKED_SOLO_5x5.name())) {
                solo.add(leagueSummonerResponse);
            } else {
                flex.add(leagueSummonerResponse);
            }
        }

        return new LeagueResponse(
                soloLeague,
                flexLeague,
                solo,
                flex
        );
    }

}
