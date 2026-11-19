package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.TeamNameHistoryCommand;
import com.example.lolserver.esports.domain.TeamName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TeamNameHistoryRequest(
        @NotBlank @Size(max = 20) String teamCode,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String nameEng,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String darkImageUrl,
        @NotNull LocalDate validFrom,
        @NotNull LocalDate validTo,
        @Size(max = 200) String note
) {
    public TeamNameHistoryCommand toCommand() {
        return new TeamNameHistoryCommand(new TeamName(teamCode, name, nameEng, imageUrl, darkImageUrl),
                validFrom, validTo, note);
    }
}
