package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.SeasonCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateSeasonRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 80) String nameAcronym,
        Integer year,
        LocalDate startDate,
        LocalDate endDate,
        Integer sortOrder
) {
    public SeasonCommand toCommand() {
        return new SeasonCommand(null, name, nameAcronym, year, startDate, endDate,
                sortOrder != null ? sortOrder : 0);
    }
}
