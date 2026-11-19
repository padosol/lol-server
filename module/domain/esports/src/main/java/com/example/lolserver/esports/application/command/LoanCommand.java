package com.example.lolserver.esports.application.command;

import com.example.lolserver.esports.domain.vo.Position;

import java.time.LocalDate;

/** 임대. position 을 빼면 원 계약 포지션. */
public record LoanCommand(
        String teamId,
        Position position,
        LocalDate date,
        LocalDate endDate,
        String note
) {
}
