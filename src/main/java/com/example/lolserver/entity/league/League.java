package com.example.lolserver.entity.league;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

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
