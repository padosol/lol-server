package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.PersonProfileReadModel;

import java.time.LocalDate;
import java.util.List;

public record PersonProfileResponse(
        String personId,
        String nickName,
        String realName,
        String realNameEng,
        LocalDate birthDate,
        String nationality,
        String imageUrl,
        PersonStatusResponse status,
        List<SummonerLinkResponse> summoners
) {
    public static PersonProfileResponse from(PersonProfileReadModel model) {
        return new PersonProfileResponse(
                model.personId(),
                model.nickName(),
                model.realName(),
                model.realNameEng(),
                model.birthDate(),
                model.nationality(),
                model.imageUrl(),
                model.status() != null ? PersonStatusResponse.from(model.status()) : null,
                model.summoners().stream().map(SummonerLinkResponse::from).toList());
    }
}
