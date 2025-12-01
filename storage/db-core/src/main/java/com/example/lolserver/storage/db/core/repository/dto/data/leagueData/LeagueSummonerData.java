package com.example.lolserver.storage.db.core.repository.dto.data.leagueData;

import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummoner;
import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummonerDetail;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeagueSummonerData {

    private String leagueType;
    private int leaguePoints;
    private int wins;
    private int losses;
    private String oow;
    private String leagueImage;
    private String tier;
    private String rank;

    public static LeagueSummonerData of(
            LeagueSummonerDetail leagueSummonerDetail) {
        return LeagueSummonerData.builder()
                .leagueType(leagueSummonerDetail.getLeagueSummoner().getLeague().getQueue().name())
                .leaguePoints(leagueSummonerDetail.getLeaguePoints())
                .wins(leagueSummonerDetail.getWins())
                .losses(leagueSummonerDetail.getLosses())
                .oow( String.format("%.2f",
                        (((double) leagueSummonerDetail.getWins() / (leagueSummonerDetail.getWins() + leagueSummonerDetail.getLosses())))*100 ) + "%" )
                .tier(leagueSummonerDetail.getLeagueSummoner().getLeague().getTier())
                .rank(leagueSummonerDetail.getRank())
                .build();
    }

}