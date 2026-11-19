package com.example.lolserver.esports.application.model.readmodel;

import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;

/** 시즌 로스터 한 명 (계약 기간 ∩ 시즌 기간). 팀명은 그 시즌 당시 이름. */
public record SeasonRosterReadModel(
        String seasonId,
        String teamId,
        String teamCodeAtSeason,
        String teamNameAtSeason,
        String personId,
        String nickName,
        Role role,
        Position position,
        ContractType contractType,
        LocalDate startDate,
        LocalDate lastDay
) {
}
