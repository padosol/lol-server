package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.PersonStatusReadModel;
import com.example.lolserver.esports.domain.vo.HiatusReason;
import com.example.lolserver.esports.domain.vo.PersonStatus;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;

public record PersonStatusResponse(
        PersonStatus status,
        String teamId,
        String contractTeamId,
        Role role,
        Position position,
        LocalDate contractLastDay,
        boolean inactive,
        HiatusReason inactiveReason,
        boolean retiredAsPlayer,
        String nextTeamId,
        LocalDate nextStartDate,
        String rightsTeamId,
        LocalDate rightsUntil
) {
    public static PersonStatusResponse from(PersonStatusReadModel model) {
        return new PersonStatusResponse(
                model.status(),
                model.teamId(),
                model.contractTeamId(),
                model.role(),
                model.position(),
                model.contractLastDay(),
                model.inactive(),
                model.inactiveReason(),
                model.retiredAsPlayer(),
                model.nextTeamId(),
                model.nextStartDate(),
                model.rightsTeamId(),
                model.rightsUntil());
    }
}
