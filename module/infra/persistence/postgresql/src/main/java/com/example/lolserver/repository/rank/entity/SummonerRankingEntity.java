package com.example.lolserver.repository.rank.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "summoner_ranking",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_puuid_queue_region",
                columnNames = {"puuid", "queue", "region"}
        )
)
@EntityListeners(AuditingEntityListener.class)
public class SummonerRankingEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "puuid", nullable = false, length = 100)
    private String puuid;

    @Column(name = "queue", nullable = false, length = 50)
    private String queue;

    @Column(name = "region", nullable = false, length = 10)
    private String platformId;

    @Column(name = "current_rank", nullable = false)
    private int currentRank;

    @Column(name = "rank_change")
    private int rankChange;

    @Column(name = "game_name", length = 50)
    private String gameName;

    @Column(name = "tag_line", length = 10)
    private String tagLine;

    @Column(name = "most_champion1")
    private String mostChampion1;

    @Column(name = "most_champion2")
    private String mostChampion2;

    @Column(name = "most_champion3")
    private String mostChampion3;

    @Column(name = "wins", nullable = false)
    private int wins;

    @Column(name = "losses", nullable = false)
    private int losses;

    @Column(name = "win_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal winRate;

    @Column(name = "tier", nullable = false, length = 20)
    private String tier;

    @Column(name = "rank", length = 5)
    private String rank;

    @Column(name = "league_points", nullable = false)
    private int leaguePoints;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
