package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.RosterEntryReadModel;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.HiatusReason;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;

public record RosterEntryResponse(
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
    public static RosterEntryResponse from(RosterEntryReadModel model) {
        return new RosterEntryResponse(
                model.personId(),
                model.nickName(),
                model.imageUrl(),
                model.nationality(),
                model.role(),
                model.position(),
                model.contractType(),
                model.startDate(),
                model.lastDay(),
                model.onLoan(),
                model.loanFromTeamId(),
                model.inactive(),
                model.inactiveReason());
    }
}
