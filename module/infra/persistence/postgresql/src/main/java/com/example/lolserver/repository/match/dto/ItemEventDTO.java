package com.example.lolserver.repository.match.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

@Getter
public class ItemEventDTO {

    private String matchId;
    private int itemId;
    private int participantId;
    private long timestamp;
    private String type;

    @QueryProjection
    public ItemEventDTO(
            String matchId, int itemId,
            int participantId, long timestamp,
            String type
    ) {
        this.matchId = matchId;
        this.itemId = itemId;
        this.participantId = participantId;
        this.timestamp = timestamp;
        this.type = type;
    }
}
