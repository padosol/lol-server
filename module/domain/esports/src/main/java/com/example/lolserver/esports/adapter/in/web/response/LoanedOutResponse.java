package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.LoanedOutReadModel;
import com.example.lolserver.esports.domain.vo.Position;

import java.time.LocalDate;

public record LoanedOutResponse(
        String personId,
        String nickName,
        Position position,
        String loanTeamId,
        LocalDate loanLastDay
) {
    public static LoanedOutResponse from(LoanedOutReadModel model) {
        return new LoanedOutResponse(
                model.personId(),
                model.nickName(),
                model.position(),
                model.loanTeamId(),
                model.loanLastDay());
    }
}
