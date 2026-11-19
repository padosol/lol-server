package com.example.lolserver.esports.application.model.readmodel;

import java.time.LocalDate;

/** 로스터 오입력 검출 (v_esports_roster_invalid) 한 줄. */
public record RosterIssueReadModel(
        String problem,
        String personId,
        Long contractId,
        String teamId,
        LocalDate atDate
) {
}
