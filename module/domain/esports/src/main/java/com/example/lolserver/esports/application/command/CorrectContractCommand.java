package com.example.lolserver.esports.application.command;

import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.Position;

import java.time.LocalDate;

/** 계약 오입력 정정. 정정 가능한 필드 전체를 보낸 값으로 덮어쓴다. */
public record CorrectContractCommand(
        LocalDate startDate,
        LocalDate endDate,
        LocalDate actualEndDate,
        EndReason endReason,
        Position position,
        String note
) {
}
