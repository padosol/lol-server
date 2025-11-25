package com.example.lolserver.storage.db.core.repository.league.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@NoArgsConstructor
@Table(name = "league_summoner_detail")
@AllArgsConstructor
public class LeagueSummonerDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "league_summoner_id")
    private Long leagueSummonerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "league_summoner_id",
            referencedColumnName = "league_summoner_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private LeagueSummoner leagueSummoner;

    private int leaguePoints;
    private String rank;
    private int wins;
    private int losses;
    private boolean veteran;
    private boolean inactive;
    private boolean freshBlood;
    private boolean hotStreak;
}
