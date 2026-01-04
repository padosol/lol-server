package com.example.lolserver.domain.match.domain.gameData;

import com.example.lolserver.domain.match.domain.gameData.timeline.events.ItemEvents;
import com.example.lolserver.domain.match.domain.gameData.timeline.events.SkillEvents;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeqTypeData {

    private Long minute;
    private int id;
    private String type;

    public SeqTypeData(ItemEvents itemEvents) {
        this.id = itemEvents.getItemId();
        this.type = itemEvents.getType();
        this.minute = itemEvents.getTimestamp() / 1000 / 60;
    }

    public SeqTypeData(SkillEvents skillEvents) {
        this.id = skillEvents.getSkillSlot();
        this.type = skillEvents.getType();
        this.minute = skillEvents.getTimestamp() / 1000 / 60;
    }
}
