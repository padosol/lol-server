package com.example.lolserver.esports.application.command;

import com.example.lolserver.esports.domain.TeamName;

import java.time.LocalDate;

/** 개명. previousNameFrom 은 현재 이름 시작일을 모를 때만 쓴다. */
public record RenameTeamCommand(
        LocalDate renamedOn,
        TeamName newName,
        LocalDate previousNameFrom,
        String note
) {
}
