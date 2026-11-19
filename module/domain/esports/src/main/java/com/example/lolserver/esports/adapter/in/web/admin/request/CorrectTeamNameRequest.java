package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.CorrectTeamNameCommand;
import com.example.lolserver.esports.domain.TeamName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CorrectTeamNameRequest(
        @NotBlank @Size(max = 20) String teamCode,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String nameEng,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String darkImageUrl,
        LocalDate nameSince
) {
    public CorrectTeamNameCommand toCommand() {
        return new CorrectTeamNameCommand(new TeamName(teamCode, name, nameEng, imageUrl, darkImageUrl), nameSince);
    }
}
