package com.example.lolserver.repository.match.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

@Getter
public class SkillEventDTO {

    private String matchId;
    private int skillSlot;
    private int participantId;
    private long timestamp;

    @QueryProjection
    public SkillEventDTO(
            String matchId, int skillSlot,
            int participantId, long timestamp
    ) {
        this.matchId = matchId;
        this.skillSlot = skillSlot;
        this.participantId = participantId;
        this.timestamp = timestamp;
    }
}
