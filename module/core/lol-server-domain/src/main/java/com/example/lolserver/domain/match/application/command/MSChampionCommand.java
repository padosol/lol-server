package com.example.lolserver.domain.match.application.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MSChampionCommand {

    private String puuid;
    private Integer season;
    private String platform;

}
