package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.TeamRosterReadModel;

import java.util.List;

public record TeamRosterResponse(
        String teamId,
        List<RosterEntryResponse> players,
        List<RosterEntryResponse> staff,
        List<LoanedOutResponse> loanedOut
) {
    public static TeamRosterResponse from(TeamRosterReadModel model) {
        return new TeamRosterResponse(
                model.teamId(),
                model.players().stream().map(RosterEntryResponse::from).toList(),
                model.staff().stream().map(RosterEntryResponse::from).toList(),
                model.loanedOut().stream().map(LoanedOutResponse::from).toList());
    }
}
