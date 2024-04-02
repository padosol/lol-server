package com.example.lolserver.riot.dto.match;

import com.example.lolserver.web.match.entity.MatchTeam;
import com.example.lolserver.web.match.entity.MatchTeamBan;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BanDto {
    private	int championId;
    private	int pickTurn;

    public MatchTeamBan toEntity(MatchTeam matchTeam) {
        return MatchTeamBan.builder()
                .championId(championId)
                .pickTurn(pickTurn)
                .matchTeam(matchTeam)
                .build();
    }
}
