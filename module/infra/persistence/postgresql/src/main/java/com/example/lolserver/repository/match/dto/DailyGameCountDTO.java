package com.example.lolserver.repository.match.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DailyGameCountDTO {

    private LocalDate gameDate;
    private long gameCount;

    @QueryProjection
    public DailyGameCountDTO(LocalDate gameDate, long gameCount) {
        this.gameDate = gameDate;
        this.gameCount = gameCount;
    }
}
