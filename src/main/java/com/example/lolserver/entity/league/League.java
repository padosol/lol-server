package com.example.lolserver.entity.league;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
    private String leagueId;

    private String tier;
    private String name;

    @Enumerated(EnumType.STRING)
    private QueueType queue;


}
