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
@Table(name = "esports_season")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EsportsSeasonEntity {

    @Id
    @Column(name = "season_id", length = 80)
    private String seasonId;

    @Column(name = "league_id", nullable = false, length = 50)
    private String leagueId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "name_acronym", length = 80)
    private String nameAcronym;

    @Column(name = "year")
    private Integer year;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
