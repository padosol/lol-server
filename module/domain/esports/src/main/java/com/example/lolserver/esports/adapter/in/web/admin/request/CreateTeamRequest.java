package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.CreateTeamCommand;
import com.example.lolserver.esports.domain.TeamName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTeamRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9_]{1,30}$") String teamId,
        @NotBlank @Size(max = 20) String teamCode,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String nameEng,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String darkImageUrl,
        @Size(max = 50) String homeLeagueId,
        @Size(max = 30) String parentTeamId,
        LocalDate foundedAt,
        LocalDate nameSince
) {
    public CreateTeamCommand toCommand() {
        return new CreateTeamCommand(teamId, new TeamName(teamCode, name, nameEng, imageUrl, darkImageUrl),
                homeLeagueId, parentTeamId, foundedAt, nameSince);
    }
}
