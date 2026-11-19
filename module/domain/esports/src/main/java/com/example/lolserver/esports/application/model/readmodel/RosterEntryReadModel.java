package com.example.lolserver.esports.application.model.readmodel;

import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.HiatusReason;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;

/** 현재 로스터 한 명. 임대 온 선수는 onLoan + 원 소속팀. */
public record RosterEntryReadModel(
        String personId,
        String nickName,
        String imageUrl,
        String nationality,
        Role role,
        Position position,
        ContractType contractType,
        LocalDate startDate,
        LocalDate lastDay,
        boolean onLoan,
        String loanFromTeamId,
        boolean inactive,
        HiatusReason inactiveReason
) {
}
