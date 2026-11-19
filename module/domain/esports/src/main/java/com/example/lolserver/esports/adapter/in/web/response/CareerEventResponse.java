package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.CareerEventReadModel;
import com.example.lolserver.esports.domain.vo.CareerEventType;
import com.example.lolserver.esports.domain.vo.HiatusReason;

import java.time.LocalDate;

public record CareerEventResponse(
        Long eventId,
        CareerEventType eventType,
        LocalDate eventDate,
        String teamId,
        HiatusReason reason,
        String note,
        String sourceUrl
) {
    public static CareerEventResponse from(CareerEventReadModel model) {
        return new CareerEventResponse(
                model.eventId(),
                model.eventType(),
                model.eventDate(),
                model.teamId(),
                model.reason(),
                model.note(),
                model.sourceUrl());
    }
}
