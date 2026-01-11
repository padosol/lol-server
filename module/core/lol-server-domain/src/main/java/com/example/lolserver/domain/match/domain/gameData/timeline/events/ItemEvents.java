package com.example.lolserver.domain.match.domain.gameData.timeline.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemEvents {
    private int itemId;
    private int participantId;
    private long timestamp;
    private String type;
    private int afterId;
    private int beforeId;
    private int goldGain;
}
