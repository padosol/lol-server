package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.LeagueCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLeagueRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 50) String nameAcronym,
        @Size(max = 50) String region,
        boolean international,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String darkImageUrl,
        Integer sortOrder,
        Boolean active
) {
    public LeagueCommand toCommand() {
        return new LeagueCommand(null, name, nameAcronym, region, international, imageUrl, darkImageUrl,
                sortOrder != null ? sortOrder : 0, active == null || active);
    }
}
