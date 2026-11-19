package com.example.lolserver.esports.application.command;

import com.example.lolserver.esports.domain.vo.HiatusReason;

import java.time.LocalDate;

/** 활동 중단. */
public record HiatusCommand(
        LocalDate date,
        HiatusReason reason,
        String note,
        String sourceUrl
) {
}
