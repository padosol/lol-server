package com.example.lolserver.repository.league.entity;

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
@Table(name = "league")
public class LeagueEntity {

    @Id
    @Column(name = "league_id")
    private String leagueId;

    private String tier;
    private String name;

    private String queue;
}
