package com.example.lolserver.repository.match.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TimelineEventDTO {

    public static final String SOURCE_ITEM = "ITEM";
    public static final String SOURCE_SKILL = "SKILL";

    private String matchId;
    private int participantId;
    private int eventId;
    private String eventType;
    private long timestamp;
    private String eventSource;
}
