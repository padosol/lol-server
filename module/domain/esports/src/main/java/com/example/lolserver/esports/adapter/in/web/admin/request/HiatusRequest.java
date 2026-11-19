package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.HiatusCommand;
import com.example.lolserver.esports.domain.vo.HiatusReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** 활동 중단. reason: REST 휴식·부상 / SUSPENSION 출장정지 / MILITARY 군 입대 / PERSONAL 개인 사유. */
public record HiatusRequest(
        @NotNull LocalDate date,
        HiatusReason reason,
        @Size(max = 300) String note,
        @Size(max = 500) String sourceUrl
) {
    public HiatusCommand toCommand() {
        return new HiatusCommand(date, reason, note, sourceUrl);
    }
}
