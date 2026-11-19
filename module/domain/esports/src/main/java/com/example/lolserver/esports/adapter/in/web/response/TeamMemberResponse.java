package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.TeamMemberReadModel;

import java.time.LocalDate;

public record TeamMemberResponse(
        TenureResponse tenure,
        LocalDate teamStartDate,
        LocalDate teamLastDay
) {
    public static TeamMemberResponse from(TeamMemberReadModel model) {
        return new TeamMemberResponse(
                model.tenure() != null ? TenureResponse.from(model.tenure()) : null,
                model.teamStartDate(),
                model.teamLastDay());
    }
}
