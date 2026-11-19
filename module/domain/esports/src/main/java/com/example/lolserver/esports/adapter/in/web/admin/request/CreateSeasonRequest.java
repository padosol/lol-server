package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.SeasonCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateSeasonRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9_]{1,80}$") String seasonId,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 80) String nameAcronym,
        Integer year,
        LocalDate startDate,
        LocalDate endDate,
        Integer sortOrder
) {
    public SeasonCommand toCommand() {
        return new SeasonCommand(seasonId, name, nameAcronym, year, startDate, endDate,
                sortOrder != null ? sortOrder : 0);
    }
}
