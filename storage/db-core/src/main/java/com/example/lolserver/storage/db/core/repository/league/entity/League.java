package com.example.lolserver.storage.db.core.repository.league.entity;

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
public class League {

    @Id
    @Column(name = "league_id")
    private String leagueId;

    private String tier;
    private String name;

    private String queue;
}
