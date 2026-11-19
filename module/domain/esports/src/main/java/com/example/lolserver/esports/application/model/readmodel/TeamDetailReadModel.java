package com.example.lolserver.esports.application.model.readmodel;

import java.util.List;

/** 팀 상세. organizationTeams 는 같은 조직의 다른 팀(자기 제외). */
public record TeamDetailReadModel(
        TeamReadModel team,
        List<TeamNameReadModel> pastNames,
        List<TeamReadModel> organizationTeams
) {
}
