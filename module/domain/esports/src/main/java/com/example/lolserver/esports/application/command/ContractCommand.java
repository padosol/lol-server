package com.example.lolserver.esports.application.command;

import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;

/** 입단·재계약·이적. date 는 효력 발생일, endDate 는 계약서상 만료일(GCD). */
public record ContractCommand(
        String teamId,
        Role role,
        Position position,
        LocalDate date,
        LocalDate endDate,
        String note
) {
}
