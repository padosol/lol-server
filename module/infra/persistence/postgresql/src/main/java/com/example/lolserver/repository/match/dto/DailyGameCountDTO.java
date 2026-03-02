package com.example.lolserver.repository.match.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DailyGameCountDTO {

    private LocalDate gameDate;
    private Long gameCount;

    public DailyGameCountDTO(LocalDate gameDate, Long gameCount) {
        this.gameDate = gameDate;
        this.gameCount = gameCount;
    }
}
