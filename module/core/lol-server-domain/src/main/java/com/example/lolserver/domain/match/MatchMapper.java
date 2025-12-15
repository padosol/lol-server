package com.example.lolserver.domain.match;

import com.example.lolserver.domain.match.domain.gameData.SeqTypeData;
import com.example.lolserver.repository.match.entity.MatchEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchMapper {
    public static Map<Integer, Map<String, List<SeqTypeData>>> domainToTimeLineDataMap(MatchEntity match) {
        Map<Integer, Map<String, List<SeqTypeData>>> timelineMap = new HashMap<>();
        return timelineMap;
    }

    public Map<Integer, Map<String, List<SeqTypeData>>> getTimelineDataMap() {
        Map<Integer, Map<String, List<SeqTypeData>>> timelineMap = new HashMap<>();

        // 타임라인 데이터s
//        for (TimeLineEvent timeLineEvent : this.timeLineEvents) {
//
//            List<ItemEvents> itemEvents = timeLineEvent.getItemEvents();
//            for (ItemEvents itemEvent : itemEvents) {
//
//                if(!itemEvent.getType().equalsIgnoreCase("ITEM_PURCHASED")) continue;
//
//                int participantId = itemEvent.getParticipantId();
//                if(!timelineMap.containsKey(participantId)) {
//                    timelineMap.put(participantId, new HashMap<>());
//                }
//
//                if(!timelineMap.get(participantId).containsKey(SeqType.ITEM_SEQ.name())) {
//                    timelineMap.get(participantId).put(SeqType.ITEM_SEQ.name(), new ArrayList<>());
//                }
//
//                List<SeqTypeData> itemSeq = timelineMap.get(participantId).get(SeqType.ITEM_SEQ.name());
//                itemSeq.add(new SeqTypeData(itemEvent));
//            }
//
//            List<SkillEvents> skillEvents = timeLineEvent.getSkillEvents();
//            for (SkillEvents skillEvent : skillEvents) {
//                int participantId = skillEvent.getParticipantId();
//                if(!timelineMap.containsKey(participantId)) {
//                    timelineMap.put(participantId, new HashMap<>());
//                }
//
//                if(!timelineMap.get(participantId).containsKey(SeqType.SKILL_SEQ.name())) {
//                    timelineMap.get(participantId).put(SeqType.SKILL_SEQ.name(), new ArrayList<>());
//                }
//
//                List<SeqTypeData> skillSeq = timelineMap.get(participantId).get(SeqType.SKILL_SEQ.name());
//                skillSeq.add(new SeqTypeData(skillEvent));
//            }
//        }

        return timelineMap;
    }
}
