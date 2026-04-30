package com.example.lolserver.repository.match.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TimelineEventDTO {

    public static final String TYPE_SKILL_LEVEL_UP = "SKILL_LEVEL_UP";
    public static final String TYPE_PREFIX_ITEM = "ITEM_";

    private String matchId;
    private int participantId;
    private String type;
    private Integer itemId;
    private Integer skillSlot;
    private String levelUpType;
    private Integer beforeId;
    private Integer afterId;
    private Integer goldGain;
    private long timestamp;

    public boolean isSkillEvent() {
        return TYPE_SKILL_LEVEL_UP.equals(type);
    }

    public boolean isItemEvent() {
        return type != null && type.startsWith(TYPE_PREFIX_ITEM);
    }
}
