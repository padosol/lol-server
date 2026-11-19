package com.example.lolserver.esports.application.model.readmodel;

import java.util.List;

/** 커리어 — 소속 기간·계약·사건, 모두 시간순. */
public record PersonCareerReadModel(
        String personId,
        List<TenureReadModel> tenures,
        List<ContractReadModel> contracts,
        List<CareerEventReadModel> events
) {
}
