package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.ContractReadModel;
import com.example.lolserver.esports.domain.vo.ContractStatus;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;

public record ContractResponse(
        Long contractId,
        String teamId,
        String teamCode,
        Role role,
        Position position,
        ContractType contractType,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate actualEndDate,
        LocalDate lastDay,
        ContractStatus status,
        EndReason endReason,
        String note
) {
    public static ContractResponse from(ContractReadModel model) {
        return new ContractResponse(
                model.contractId(),
                model.teamId(),
                model.teamCode(),
                model.role(),
                model.position(),
                model.contractType(),
                model.startDate(),
                model.endDate(),
                model.actualEndDate(),
                model.lastDay(),
                model.status(),
                model.endReason(),
                model.note());
    }
}
