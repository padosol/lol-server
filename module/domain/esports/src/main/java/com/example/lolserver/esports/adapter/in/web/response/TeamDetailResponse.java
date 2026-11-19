package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.TeamDetailReadModel;

import java.util.List;

public record TeamDetailResponse(
        TeamResponse team,
        List<TeamNameResponse> pastNames,
        List<TeamResponse> organizationTeams
) {
    public static TeamDetailResponse from(TeamDetailReadModel model) {
        return new TeamDetailResponse(
                model.team() != null ? TeamResponse.from(model.team()) : null,
                model.pastNames().stream().map(TeamNameResponse::from).toList(),
                model.organizationTeams().stream().map(TeamResponse::from).toList());
    }
}
