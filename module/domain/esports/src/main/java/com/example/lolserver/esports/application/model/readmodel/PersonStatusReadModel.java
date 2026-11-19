package com.example.lolserver.esports.application.model.readmodel;

import com.example.lolserver.esports.domain.vo.HiatusReason;
import com.example.lolserver.esports.domain.vo.PersonStatus;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;

/** 인물 현재 상태와 보조 정보 (v_esports_person_status). */
public record PersonStatusReadModel(
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
}
