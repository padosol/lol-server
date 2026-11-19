package com.example.lolserver.esports.application.command;

import com.example.lolserver.esports.domain.TeamName;

import java.time.LocalDate;

/** 현재 이름 오입력 정정 (이력을 남기지 않는다). */
public record CorrectTeamNameCommand(
        TeamName name,
        LocalDate nameSince
) {
}
