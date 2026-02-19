package com.example.lolserver.domain.match.domain;

import com.example.lolserver.domain.match.domain.gamedata.timeline.ItemSeqData;
import com.example.lolserver.domain.match.domain.gamedata.timeline.ParticipantTimeline;
import com.example.lolserver.domain.match.domain.gamedata.timeline.SkillSeqData;
import com.example.lolserver.domain.match.domain.gamedata.timeline.events.ItemEvents;
import com.example.lolserver.domain.match.domain.gamedata.timeline.events.SkillEvents;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TimelineData {

    private Map<Integer, ParticipantTimeline> participants = new HashMap<>();

    public TimelineData(List<ItemEvents> itemEvents, List<SkillEvents> skillEvents) {
        this.participants = buildParticipants(itemEvents, skillEvents);
    }

    private Map<Integer, ParticipantTimeline> buildParticipants(
            List<ItemEvents> itemEvents, List<SkillEvents> skillEvents) {
        Map<Integer, ParticipantTimeline> participantMap = new HashMap<>();

        for (ItemEvents itemEvent : itemEvents) {
            if (!itemEvent.getType().equalsIgnoreCase("ITEM_PURCHASED")) {
                continue;
            }

            int participantId = itemEvent.getParticipantId();
            participantMap.computeIfAbsent(participantId, k -> new ParticipantTimeline());
            participantMap.get(participantId).addItemSeq(new ItemSeqData(itemEvent));
        }

        for (SkillEvents skillEvent : skillEvents) {
            int participantId = skillEvent.getParticipantId();
            participantMap.computeIfAbsent(participantId, k -> new ParticipantTimeline());
            participantMap.get(participantId).addSkillSeq(new SkillSeqData(skillEvent));
        }

        return participantMap;
    }

    public ParticipantTimeline getParticipantTimeline(int participantId) {
        return participants.get(participantId);
    }
}
