package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.MoveCommand;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** 팀 내 이동. 만료일은 직전 계약에서 물려받으므로 받지 않는다. */
public record MoveRequest(
        @NotBlank String teamId,
        @NotNull Role role,
        Position position,
        @NotNull LocalDate date,
        @Size(max = 300) String note
) {
    public MoveCommand toCommand() {
        return new MoveCommand(teamId, role, position, date, note);
    }
}
