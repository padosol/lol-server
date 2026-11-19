package com.example.lolserver.esports.application.model.readmodel;

import java.time.LocalDate;

/** 지나간 팀 이름. 기간은 양 끝 포함. */
public record TeamNameReadModel(
        Long historyId,
        String teamCode,
        String name,
        String nameEng,
        String imageUrl,
        String darkImageUrl,
        LocalDate validFrom,
        LocalDate validTo
) {
}
