package com.example.lolserver.esports.application.command;

import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;

/** 팀 내 이동. 만료일은 직전 계약에서 물려받는다. */
public record MoveCommand(
        String teamId,
        Role role,
        Position position,
        LocalDate date,
        String note
) {
}
