package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.PersonCareerReadModel;

import java.util.List;

public record PersonCareerResponse(
        String personId,
        List<TenureResponse> tenures,
        List<ContractResponse> contracts,
        List<CareerEventResponse> events
) {
    public static PersonCareerResponse from(PersonCareerReadModel model) {
        return new PersonCareerResponse(
                model.personId(),
                model.tenures().stream().map(TenureResponse::from).toList(),
                model.contracts().stream().map(ContractResponse::from).toList(),
                model.events().stream().map(CareerEventResponse::from).toList());
    }
}
