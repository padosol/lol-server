package com.example.lolserver.repository.tiercutoff.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tier_cutoff")
public class TierCutoffEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "queue", nullable = false, length = 50)
    private String queue;

    @Column(name = "tier", nullable = false, length = 20)
    private String tier;

    @Column(name = "region", nullable = false, length = 10)
    private String region;

    @Column(name = "min_league_points", nullable = false)
    private int minLeaguePoints;

    @Column(name = "lp_change")
    private int lpChange;

    @Column(name = "user_count")
    private int userCount;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
