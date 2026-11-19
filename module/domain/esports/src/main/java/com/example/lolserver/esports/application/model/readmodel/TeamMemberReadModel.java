package com.example.lolserver.esports.application.model.readmodel;

import java.time.LocalDate;

/** 팀 역대 인원 — 소속 기간과 그 안의 이 팀 구간. */
public record TeamMemberReadModel(
        TenureReadModel tenure,
        LocalDate teamStartDate,
        LocalDate teamLastDay
) {
}
