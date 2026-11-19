package com.example.lolserver.esports.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "esports_team")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EsportsTeamEntity {

    @Id
    @Column(name = "team_id", length = 30)
    private String teamId;

    @Column(name = "team_code", nullable = false, length = 20)
    private String teamCode;

    @Column(name = "game_code", nullable = false, length = 20)
    private String gameCode;

    @Column(name = "home_league_id", length = 50)
    private String homeLeagueId;

    @Column(name = "parent_team_id", length = 30)
    private String parentTeamId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "name_eng", length = 100)
    private String nameEng;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "dark_image_url", length = 500)
    private String darkImageUrl;

    @Column(name = "founded_at")
    private LocalDate foundedAt;

    @Column(name = "disbanded_at")
    private LocalDate disbandedAt;

    @Column(name = "name_since")
    private LocalDate nameSince;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
