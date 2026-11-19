package com.example.lolserver.esports.application.model.readmodel;

import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;
import java.util.List;

/** 소속 기간 — 같은 조직의 연속 계약을 묶은 커리어 한 줄. */
public record TenureReadModel(
        String personId,
        String nickName,
        String organizationId,
        boolean asPlayer,
        LocalDate startDate,
        LocalDate lastDay,
        EndReason endReason,
        int contractCount,
        int renewalCount,
        boolean loan,
        List<Role> roles,
        List<String> teamIds
) {
}
