package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.TeamReadModel;

import java.time.LocalDate;

public record TeamResponse(
        String teamId,
        String teamCode,
        String name,
        String nameEng,
        String imageUrl,
        String darkImageUrl,
        String homeLeagueId,
        String parentTeamId,
        LocalDate foundedAt,
        LocalDate disbandedAt,
        LocalDate nameSince
) {
    public static TeamResponse from(TeamReadModel model) {
        return new TeamResponse(
                model.teamId(),
                model.teamCode(),
                model.name(),
                model.nameEng(),
                model.imageUrl(),
                model.darkImageUrl(),
                model.homeLeagueId(),
                model.parentTeamId(),
                model.foundedAt(),
                model.disbandedAt(),
                model.nameSince());
    }
}
