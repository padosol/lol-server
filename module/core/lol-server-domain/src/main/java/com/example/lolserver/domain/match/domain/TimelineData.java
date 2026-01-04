package com.example.lolserver.domain.match.domain;

import com.example.lolserver.domain.match.domain.gameData.SeqTypeData;
import com.example.lolserver.domain.match.domain.gameData.seqType.SeqType;
import com.example.lolserver.domain.match.domain.gameData.timeline.events.ItemEvents;
import com.example.lolserver.domain.match.domain.gameData.timeline.events.SkillEvents;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimelineData {

    private Map<Integer, Map<String, List<SeqTypeData>>> data;

    public TimelineData(List<ItemEvents> itemEvents, List<SkillEvents> skillEvents) {
        this.data = getTimelineDataMap(itemEvents, skillEvents);
    }

    public Map<Integer, Map<String, List<SeqTypeData>>> getTimelineDataMap(List<ItemEvents> itemEvents, List<SkillEvents> skillEvents) {
        Map<Integer, Map<String, List<SeqTypeData>>> timelineMap = new HashMap<>();

        // 타임라인 데이터
        for (ItemEvents itemEvent : itemEvents) {

            if(!itemEvent.getType().equalsIgnoreCase("ITEM_PURCHASED")) continue;

            int participantId = itemEvent.getParticipantId();
            if(!timelineMap.containsKey(participantId)) {
                timelineMap.put(participantId, new HashMap<>());
            }

            if(!timelineMap.get(participantId).containsKey(SeqType.ITEM_SEQ.name())) {
                timelineMap.get(participantId).put(SeqType.ITEM_SEQ.name(), new ArrayList<>());
            }

            List<SeqTypeData> itemSeq = timelineMap.get(participantId).get(SeqType.ITEM_SEQ.name());
            itemSeq.add(new SeqTypeData(itemEvent));
        }

        for (SkillEvents skillEvent : skillEvents) {
            int participantId = skillEvent.getParticipantId();
            if(!timelineMap.containsKey(participantId)) {
                timelineMap.put(participantId, new HashMap<>());
            }

            if(!timelineMap.get(participantId).containsKey(SeqType.SKILL_SEQ.name())) {
                timelineMap.get(participantId).put(SeqType.SKILL_SEQ.name(), new ArrayList<>());
            }

            List<SeqTypeData> skillSeq = timelineMap.get(participantId).get(SeqType.SKILL_SEQ.name());
            skillSeq.add(new SeqTypeData(skillEvent));
        }

        return timelineMap;
    }
}
