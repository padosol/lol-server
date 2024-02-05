package com.example.lolserver.entity.league;


import com.example.lolserver.entity.summoner.Summoner;
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

    @Id
    @GeneratedValue
    @Column(name = "league_summoner_id")
    private Long id;

    private int leaguePoints;
    private String rank;
    private int wins;
    private int losses;
    private boolean veteran;
    private boolean inactive;
    private boolean freshBlood;
    private boolean hotStreak;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summoner_id")
    private Summoner summoner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id")
    private League league;

    public LeagueSummonerData toData() {
        return LeagueSummonerData.builder()
                .leagueType(league.getQueue().name())
                .leaguePoints(leaguePoints)
                .wins(wins)
                .losses(losses)
                .oow( String.format("%.2f", (((double) wins / (wins + losses)))*100 ) + "%" )
                .tier(league.getTier())
                .build();
    }

}
