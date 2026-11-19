package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.SeasonRosterReadModel;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;

public record SeasonRosterResponse(
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
    public static SeasonRosterResponse from(SeasonRosterReadModel model) {
        return new SeasonRosterResponse(
                model.seasonId(),
                model.teamId(),
                model.teamCodeAtSeason(),
                model.teamNameAtSeason(),
                model.personId(),
                model.nickName(),
                model.role(),
                model.position(),
                model.contractType(),
                model.startDate(),
                model.lastDay());
    }
}
