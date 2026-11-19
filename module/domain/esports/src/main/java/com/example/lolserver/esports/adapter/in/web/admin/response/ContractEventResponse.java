package com.example.lolserver.esports.adapter.in.web.admin.response;

import com.example.lolserver.esports.adapter.in.web.response.CareerEventResponse;
import com.example.lolserver.esports.adapter.in.web.response.ContractResponse;
import com.example.lolserver.esports.adapter.in.web.response.PersonStatusResponse;
import com.example.lolserver.esports.application.model.resultmodel.ContractEventResultModel;

import java.util.List;

/** 사건 처리 결과 — 저장 뒤 인물 상태와 이번에 바뀌거나 생긴 계약·사건. */
public record ContractEventResponse(
        String personId,
        PersonStatusResponse status,
        List<ContractResponse> contracts,
        List<CareerEventResponse> events
) {
    public static ContractEventResponse from(ContractEventResultModel result) {
        return new ContractEventResponse(result.personId(),
                result.status() != null ? PersonStatusResponse.from(result.status()) : null,
                result.contracts().stream().map(ContractResponse::from).toList(),
                result.events().stream().map(CareerEventResponse::from).toList());
    }
}
