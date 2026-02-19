package com.example.lolserver.repository.champion_stat.entity;

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
@Table(name = "champion_item_stat")
public class ChampionItemStatEntity {

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

    @Column(name = "build_type", nullable = false, length = 20)
    private String buildType;

    @Column(name = "item_ids", nullable = false, length = 255)
    private String itemIds;

    @Column(name = "games", nullable = false)
    private int games;

    @Column(name = "wins", nullable = false)
    private int wins;
}
