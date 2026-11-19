package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.LoanCommand;
import com.example.lolserver.esports.domain.vo.Position;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** 임대. 임대 계약은 시작·종료일이 반드시 있다. position 을 빼면 원 계약 포지션. */
public record LoanRequest(
        @NotBlank String teamId,
        Position position,
        @NotNull LocalDate date,
        @NotNull LocalDate endDate,
        @Size(max = 300) String note
) {
    public LoanCommand toCommand() {
        return new LoanCommand(teamId, position, date, endDate, note);
    }
}
