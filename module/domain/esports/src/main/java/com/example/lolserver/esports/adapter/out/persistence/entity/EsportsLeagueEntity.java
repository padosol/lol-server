package com.example.lolserver.esports.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "esports_league")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EsportsLeagueEntity {

    @Id
    @Column(name = "league_id", length = 50)
    private String leagueId;

    @Column(name = "game_code", nullable = false, length = 20)
    private String gameCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "name_acronym", length = 50)
    private String nameAcronym;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "international", nullable = false)
    private boolean international;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "dark_image_url", length = 500)
    private String darkImageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
