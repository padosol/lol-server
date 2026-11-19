package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.UpdateTeamCommand;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** 이름·코드·로고는 개명({@code /renames})과 정정({@code /current-name})으로만 바꾼다. */
public record UpdateTeamRequest(
        @Size(max = 50) String homeLeagueId,
        @Size(max = 30) String parentTeamId,
        LocalDate foundedAt
) {
    public UpdateTeamCommand toCommand() {
        return new UpdateTeamCommand(homeLeagueId, parentTeamId, foundedAt);
    }
}
