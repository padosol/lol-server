package com.example.lolserver.esports.application.model.readmodel;

import java.time.LocalDate;
import java.util.List;

/** 인물 프로필. summoners 는 공개 계정만, 대표 계정 먼저. */
public record PersonProfileReadModel(
        String personId,
        String nickName,
        String realName,
        String realNameEng,
        LocalDate birthDate,
        String nationality,
        String imageUrl,
        PersonStatusReadModel status,
        List<SummonerLinkReadModel> summoners
) {
}
