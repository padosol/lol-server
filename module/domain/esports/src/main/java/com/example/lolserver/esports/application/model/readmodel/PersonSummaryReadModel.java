package com.example.lolserver.esports.application.model.readmodel;

/** 인물 요약. 계약·사건 이력이 없으면 status·teamCode 가 null. */
public record PersonSummaryReadModel(
        String personId,
        String nickName,
        String realName,
        String nationality,
        String imageUrl,
        String teamCode,
        PersonStatusReadModel status
) {
}
