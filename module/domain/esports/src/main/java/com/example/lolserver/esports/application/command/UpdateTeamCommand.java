package com.example.lolserver.esports.application.command;

import java.time.LocalDate;

/** 팀 수정. 이름·코드·로고는 개명·정정으로만 바꾼다. */
public record UpdateTeamCommand(
        String homeLeagueId,
        String parentTeamId,
        LocalDate foundedAt
) {
}
