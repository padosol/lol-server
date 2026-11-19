package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.RenameTeamCommand;
import com.example.lolserver.esports.domain.TeamName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** {@code previousNameFrom} 은 현재 이름 시작일(nameSince·foundedAt)을 모를 때만 넣는다. */
public record RenameTeamRequest(
        @NotNull LocalDate renamedOn,
        @NotBlank @Size(max = 20) String teamCode,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String nameEng,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String darkImageUrl,
        LocalDate previousNameFrom,
        @Size(max = 200) String note
) {
    public RenameTeamCommand toCommand() {
        return new RenameTeamCommand(renamedOn, new TeamName(teamCode, name, nameEng, imageUrl, darkImageUrl),
                previousNameFrom, note);
    }
}
