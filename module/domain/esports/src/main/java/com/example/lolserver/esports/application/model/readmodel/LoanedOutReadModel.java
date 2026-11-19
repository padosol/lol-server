package com.example.lolserver.esports.application.model.readmodel;

import com.example.lolserver.esports.domain.vo.Position;

import java.time.LocalDate;

/** 이 팀 계약인데 지금 다른 팀에 임대 간 선수. */
public record LoanedOutReadModel(
        String personId,
        String nickName,
        Position position,
        String loanTeamId,
        LocalDate loanLastDay
) {
}
