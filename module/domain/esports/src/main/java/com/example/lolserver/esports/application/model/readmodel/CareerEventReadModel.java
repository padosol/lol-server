package com.example.lolserver.esports.application.model.readmodel;

import com.example.lolserver.esports.domain.CareerEvent;
import com.example.lolserver.esports.domain.vo.CareerEventType;
import com.example.lolserver.esports.domain.vo.HiatusReason;

import java.time.LocalDate;

/** 커리어 사건. */
public record CareerEventReadModel(
        Long eventId,
        CareerEventType eventType,
        LocalDate eventDate,
        String teamId,
        HiatusReason reason,
        String note,
        String sourceUrl
) {
    public static CareerEventReadModel of(CareerEvent event) {
        return new CareerEventReadModel(event.getEventId(), event.getEventType(), event.getEventDate(),
                event.getTeamId(), event.getReason(), event.getNote(), event.getSourceUrl());
    }
}
