package com.example.lolserver.esports.application.command;

import java.time.LocalDate;

/** 날짜만 있는 사건 (임대 복귀·방출·은퇴·은퇴 번복·활동 재개). */
public record DatedCommand(
        LocalDate date,
        String note,
        String sourceUrl
) {
}
