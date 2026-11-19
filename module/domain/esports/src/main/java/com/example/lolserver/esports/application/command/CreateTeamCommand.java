package com.example.lolserver.esports.application.command;

import com.example.lolserver.esports.domain.TeamName;

import java.time.LocalDate;

/** 팀 등록. CL 팀은 parentTeamId 로 같은 조직의 LCK 팀을 지정한다. */
public record CreateTeamCommand(
        String teamId,
        TeamName name,
        String homeLeagueId,
        String parentTeamId,
        LocalDate foundedAt,
        LocalDate nameSince
) {
}
