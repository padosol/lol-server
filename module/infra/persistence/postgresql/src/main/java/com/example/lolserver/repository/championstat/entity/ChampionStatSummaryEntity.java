package com.example.lolserver.repository.championstat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "champion_stat_summary")
public class ChampionStatSummaryEntity {

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

    @Column(name = "total_games", nullable = false)
    private int totalGames;

    @Column(name = "wins", nullable = false)
    private int wins;

    @Column(name = "total_bans", nullable = false)
    private int totalBans;

    @Column(name = "total_matches_in_dimension", nullable = false)
    private int totalMatchesInDimension;

    @Column(name = "avg_kills", nullable = false, precision = 5, scale = 2)
    private BigDecimal avgKills;

    @Column(name = "avg_deaths", nullable = false, precision = 5, scale = 2)
    private BigDecimal avgDeaths;

    @Column(name = "avg_assists", nullable = false, precision = 5, scale = 2)
    private BigDecimal avgAssists;

    @Column(name = "avg_cs", nullable = false, precision = 7, scale = 2)
    private BigDecimal avgCs;

    @Column(name = "avg_gold", nullable = false, precision = 9, scale = 2)
    private BigDecimal avgGold;
}
