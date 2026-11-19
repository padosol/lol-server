package com.example.lolserver.esports.application.model.readmodel;

import java.util.List;

/** 팀 현재 로스터. players 는 포지션 순, staff 는 감독 먼저. */
public record TeamRosterReadModel(
        String teamId,
        List<RosterEntryReadModel> players,
        List<RosterEntryReadModel> staff,
        List<LoanedOutReadModel> loanedOut
) {
}
