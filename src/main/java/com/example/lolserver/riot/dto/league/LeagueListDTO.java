package com.example.lolserver.riot.dto.league;

import lombok.Getter;
import lombok.Setter;
import org.example.entity.league.League;
import org.example.entity.league.QueueType;

import java.util.List;

@Getter
@Setter
public class LeagueListDTO {

    private	String leagueId;
    private List<LeagueItemDTO> entries;
    private	String tier;
    private	String name;
    private	String queue;

    public League toEntity() {
        return League.builder()
                .leagueId(leagueId)
                .tier(tier)
                .name(name)
                .queue(QueueType.valueOf(queue))
                .build();
    }


}
