package com.example.lolserver.esports.application.command;

import com.example.lolserver.esports.domain.TeamName;

import java.time.LocalDate;

/** 지나간 이름 정정. */
public record TeamNameHistoryCommand(
        TeamName name,
        LocalDate validFrom,
        LocalDate validTo,
        String note
) {
}
