package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.ContractCommand;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** 입단·재계약·이적. date 는 효력 발생일, endDate 는 계약서상 만료일(GCD, 미공개면 비운다). 선수는 position 필수. */
public record ContractEventRequest(
        @NotBlank String teamId,
        @NotNull Role role,
        Position position,
        @NotNull LocalDate date,
        LocalDate endDate,
        @Size(max = 300) String note
) {
    public ContractCommand toCommand() {
        return new ContractCommand(teamId, role, position, date, endDate, note);
    }
}
