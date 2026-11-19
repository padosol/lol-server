package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.TenureReadModel;
import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;
import java.util.List;

public record TenureResponse(
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
    public static TenureResponse from(TenureReadModel model) {
        return new TenureResponse(
                model.personId(),
                model.nickName(),
                model.organizationId(),
                model.asPlayer(),
                model.startDate(),
                model.lastDay(),
                model.endReason(),
                model.contractCount(),
                model.renewalCount(),
                model.loan(),
                model.roles(),
                model.teamIds());
    }
}
