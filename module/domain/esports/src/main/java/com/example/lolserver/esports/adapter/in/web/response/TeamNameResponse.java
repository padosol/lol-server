package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.TeamNameReadModel;

import java.time.LocalDate;

public record TeamNameResponse(
        Long historyId,
        String teamCode,
        String name,
        String nameEng,
        String imageUrl,
        String darkImageUrl,
        LocalDate validFrom,
        LocalDate validTo
) {
    public static TeamNameResponse from(TeamNameReadModel model) {
        return new TeamNameResponse(
                model.historyId(),
                model.teamCode(),
                model.name(),
                model.nameEng(),
                model.imageUrl(),
                model.darkImageUrl(),
                model.validFrom(),
                model.validTo());
    }
}
