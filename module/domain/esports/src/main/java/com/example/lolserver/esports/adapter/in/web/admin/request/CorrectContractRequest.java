package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.CorrectContractCommand;
import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.Position;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 계약 오입력 정정. 정정 가능한 필드 전체를 보낸 값으로 덮어쓴다(빠진 필드는 null 이 된다).
 * 조기 종료일과 종료 사유는 짝이다. EXPIRED 는 저장하지 않는다.
 */
public record CorrectContractRequest(
        @NotNull LocalDate startDate,
        LocalDate endDate,
        LocalDate actualEndDate,
        EndReason endReason,
        Position position,
        @Size(max = 300) String note
) {
    public CorrectContractCommand toCommand() {
        return new CorrectContractCommand(startDate, endDate, actualEndDate, endReason, position, note);
    }
}
