package com.example.lolserver.domain.match.domain.gamedata.timeline.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillEvents {
    private int skillSlot;
    private int participantId;
    private String levelUpType;
    private long timestamp;
    private String type;
}
