package com.example.lolserver.match.dto.metadata;

import lombok.Data;

import java.util.List;

@Data
public class TeamDto {

    private List<BanDto> bans;
    private ObjectivesDto objectives;
    private int teamId;
    private boolean win;

}
