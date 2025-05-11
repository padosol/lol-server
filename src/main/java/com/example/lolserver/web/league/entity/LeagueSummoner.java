package com.example.lolserver.web.league.entity;


import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.web.league.entity.id.LeagueSummonerId;
import com.example.lolserver.web.dto.data.leagueData.LeagueSummonerData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "league_summoner")
public class LeagueSummoner {

    @EmbeddedId
    private LeagueSummonerId id;

    @MapsId("puuid")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puuid")
    private Summoner summoner;

    @MapsId("leagueId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id")
    private League league;

    private int leaguePoints;
    private String rank;
    private int wins;
    private int losses;
    private boolean veteran;
    private boolean inactive;
    private boolean freshBlood;
    private boolean hotStreak;

    public LeagueSummoner of(LeagueSummonerId id, League league, Summoner summoner, LeagueEntryDTO leagueEntryDTO) {
       return LeagueSummoner.builder()
                .id(id)
                .league(league)
                .summoner(summoner)
                .losses(leagueEntryDTO.getLosses())
                .leaguePoints(leagueEntryDTO.getLeaguePoints())
                .freshBlood(leagueEntryDTO.isFreshBlood())
                .hotStreak(leagueEntryDTO.isHotStreak())
                .inactive(leagueEntryDTO.isInactive())
                .rank(leagueEntryDTO.getRank())
                .veteran(leagueEntryDTO.isVeteran())
                .wins(leagueEntryDTO.getWins())
                .build();
    }

    public LeagueSummonerData toData() {
        return LeagueSummonerData.builder()
                .leagueType(league.getQueue().name())
                .leaguePoints(leaguePoints)
                .wins(wins)
                .losses(losses)
                .oow( String.format("%.2f", (((double) wins / (wins + losses)))*100 ) + "%" )
                .tier(league.getTier())
                .rank(this.rank)
                .build();
    }

    public void addLeague(League league) {
        this.league = league;
    }

}
