package com.example.lolserver.esports.application.command;

import java.time.LocalDate;

/** 시즌 등록·수정. 수정할 때 seasonId 는 경로 값을 쓴다. */
public record SeasonCommand(
        String seasonId,
        String name,
        String nameAcronym,
        Integer year,
        LocalDate startDate,
        LocalDate endDate,
        int sortOrder
) {
}
