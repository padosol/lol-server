package com.example.lolserver.esports.application.model.readmodel;

import com.example.lolserver.esports.domain.vo.ContractStatus;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;

/** 계약 (v_esports_contract_status). endReason 의 EXPIRED 는 뷰가 유도한 값. */
public record ContractReadModel(
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
}
