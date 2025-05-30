package com.example.lolserver.riot.dto.league;

import java.util.List;

import com.example.lolserver.riot.dto.error.ErrorDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeagueListDTO extends ErrorDTO {

    private	String leagueId;
    private List<LeagueItemDTO> entries;
    private	String tier;
    private	String name;
    private	String queue;



}
