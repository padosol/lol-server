package com.example.lolserver.domain.match.domain.gameData.timeline;

import com.example.lolserver.domain.match.domain.gameData.timeline.events.SkillEvents;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillSeqData {
    private int skillSlot;
    private long minute;
    private String type;

    public SkillSeqData(SkillEvents event) {
        this.skillSlot = event.getSkillSlot();
        this.minute = event.getTimestamp() / 1000 / 60;
        this.type = event.getType();
    }
}
