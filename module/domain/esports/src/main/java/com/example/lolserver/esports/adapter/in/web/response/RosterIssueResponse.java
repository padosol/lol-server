package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.RosterIssueReadModel;

import java.time.LocalDate;

public record RosterIssueResponse(
        String problem,
        String personId,
        Long contractId,
        String teamId,
        LocalDate atDate
) {
    public static RosterIssueResponse from(RosterIssueReadModel model) {
        return new RosterIssueResponse(
                model.problem(),
                model.personId(),
                model.contractId(),
                model.teamId(),
                model.atDate());
    }
}
