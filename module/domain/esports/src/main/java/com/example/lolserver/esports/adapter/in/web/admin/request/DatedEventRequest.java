package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.DatedCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** 날짜만 있는 사건 — 임대 복귀·방출·은퇴·은퇴 번복·활동 재개. */
public record DatedEventRequest(
        @NotNull LocalDate date,
        @Size(max = 300) String note,
        @Size(max = 500) String sourceUrl
) {
    public DatedCommand toCommand() {
        return new DatedCommand(date, note, sourceUrl);
    }
}
