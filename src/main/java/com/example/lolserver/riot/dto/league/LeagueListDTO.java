package com.example.lolserver.riot.dto.league;

import com.example.lolserver.entity.league.League;
import com.example.lolserver.entity.league.QueueType;
import lombok.Getter;
import lombok.Setter;

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
