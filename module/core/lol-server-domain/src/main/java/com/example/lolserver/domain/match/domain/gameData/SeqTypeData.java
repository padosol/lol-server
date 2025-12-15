package com.example.lolserver.domain.match.domain.gameData;

import com.example.lolserver.repository.match.entity.timeline.events.ItemEvents;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEvents;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeqTypeData {

    private Long minute;
    private int id;
    private String type;

    public SeqTypeData(){}

//    public SeqTypeData(EventsTimeLineDto event, String type) {
//
//        this.minute = event.getTimestamp() / 1000 / 60;
//        this.type = event.getType();
//
//        switch(EventType.valueOf(type)) {
//            case ITEM_PURCHASED -> {
//                this.id = event.getItemId();
//                break;
//            }
//
//            case SKILL_LEVEL_UP -> {
//                this.id = event.getSkillSlot();
//                break;
//            }
//        }
//    }

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
