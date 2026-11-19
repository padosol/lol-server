package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.PersonSummaryReadModel;

public record PersonSummaryResponse(
        String personId,
        String nickName,
        String realName,
        String nationality,
        String imageUrl,
        String teamCode,
        PersonStatusResponse status
) {
    public static PersonSummaryResponse from(PersonSummaryReadModel model) {
        return new PersonSummaryResponse(
                model.personId(),
                model.nickName(),
                model.realName(),
                model.nationality(),
                model.imageUrl(),
                model.teamCode(),
                model.status() != null ? PersonStatusResponse.from(model.status()) : null);
    }
}
