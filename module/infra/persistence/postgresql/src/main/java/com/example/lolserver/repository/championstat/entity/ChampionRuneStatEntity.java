package com.example.lolserver.repository.championstat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "champion_rune_stat")
public class ChampionRuneStatEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "champion_id", nullable = false)
    private int championId;

    @Column(name = "team_position", nullable = false, length = 20)
    private String teamPosition;

    @Column(name = "season", nullable = false)
    private int season;

    @Column(name = "tier_group", nullable = false, length = 30)
    private String tierGroup;

    @Column(name = "platform_id", nullable = false, length = 10)
    private String platformId;

    @Column(name = "queue_id", nullable = false)
    private int queueId;

    @Column(name = "game_version", nullable = false, length = 20)
    private String gameVersion;

    @Column(name = "primary_rune_id", nullable = false)
    private int primaryRuneId;

    @Column(name = "primary_rune_ids", nullable = false, length = 255)
    private String primaryRuneIds;

    @Column(name = "secondary_rune_id", nullable = false)
    private int secondaryRuneId;

    @Column(name = "secondary_rune_ids", nullable = false, length = 255)
    private String secondaryRuneIds;

    @Column(name = "stat_offense", nullable = false)
    private int statOffense;

    @Column(name = "stat_flex", nullable = false)
    private int statFlex;

    @Column(name = "stat_defense", nullable = false)
    private int statDefense;

    @Column(name = "games", nullable = false)
    private int games;

    @Column(name = "wins", nullable = false)
    private int wins;
}
