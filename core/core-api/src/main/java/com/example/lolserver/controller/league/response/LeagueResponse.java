package com.example.lolserver.controller.league.response;

import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummonerDetail;
import com.example.lolserver.storage.db.core.repository.league.entity.QueueType;

import java.util.ArrayList;
import java.util.List;

public record LeagueResponse(
    LeagueSummonerResponse soloLeague,
    LeagueSummonerResponse flexLeague,

    List<LeagueSummonerResponse> soloLeagueHistory,
    List<LeagueSummonerResponse> flexLeagueHistory
) {

    public static LeagueResponse of(List<LeagueSummonerDetail> leagueSummonerDetails) {
        LeagueSummonerResponse soloLeague = null;
        LeagueSummonerResponse flexLeague = null;

        List<LeagueSummonerResponse> solo = new ArrayList<>();
        List<LeagueSummonerResponse> flex = new ArrayList<>();

        for (LeagueSummonerDetail leagueSummonerDetail : leagueSummonerDetails) {
            LeagueSummonerResponse leagueSummonerResponse = new LeagueSummonerResponse(
                    leagueSummonerDetail.getLeagueSummoner().getLeague().getQueue().name(),
                    leagueSummonerDetail.getLeaguePoints(),
                    leagueSummonerDetail.getWins(),
                    leagueSummonerDetail.getLosses(),
                    ( String.format("%.2f",
                            (((double) leagueSummonerDetail.getWins() / (leagueSummonerDetail.getWins() + leagueSummonerDetail.getLosses())))*100 ) + "%" ),
                    leagueSummonerDetail.getLeagueSummoner().getLeague().getTier(),
                    leagueSummonerDetail.getRank()
            );

            if (leagueSummonerDetail.getLeagueSummoner().getLeague().getQueue().equals(QueueType.RANKED_SOLO_5x5)) {
                solo.add(leagueSummonerResponse);
            } else {
                flex.add(leagueSummonerResponse);
            }
        }

        if(!solo.isEmpty()) {
            soloLeague = solo.get(0);
        }

        if(!flex.isEmpty()) {
            flexLeague = flex.get(0);
        }

        return new LeagueResponse(
                soloLeague,
                flexLeague,
                solo,
                flex
        );
    }

}
