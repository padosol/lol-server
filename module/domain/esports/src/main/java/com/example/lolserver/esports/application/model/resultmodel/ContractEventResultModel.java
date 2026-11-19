package com.example.lolserver.esports.application.model.resultmodel;

import com.example.lolserver.esports.application.model.readmodel.CareerEventReadModel;
import com.example.lolserver.esports.application.model.readmodel.ContractReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonStatusReadModel;

import java.util.List;

/** 계약 사건 처리 결과 — 저장 뒤 인물 상태(이력이 없으면 null)와 이번에 바뀌거나 생긴 계약·사건. */
public record ContractEventResultModel(
        String personId,
        PersonStatusReadModel status,
        List<ContractReadModel> contracts,
        List<CareerEventReadModel> events
) {
}
