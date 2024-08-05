package com.example.lolserver.riot.dto.match_timeline;

import com.example.lolserver.riot.dto.error.ErrorDTO;
import com.example.lolserver.riot.dto.match_timeline.type.EventType;
import com.example.lolserver.web.dto.data.gameData.SeqTypeData;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TimelineDto extends ErrorDTO {

    private MetadataTimeLineDto metadata;
    private InfoTimeLineDto info;

    // id 와 type 별로 데이터 수집
    public Map<Integer, Map<String, List<SeqTypeData>>> dataCollection() {

        Map<Integer, Map<String, List<SeqTypeData>>> result = new HashMap<>();

        // 초기 세팅
        List<ParticipantTimeLineDto> participants = this.info.getParticipants();
        for (ParticipantTimeLineDto participant : participants) {
            int participantId = participant.getParticipantId();

            Map<String, List<SeqTypeData>> data = new HashMap<>();
            result.put(participantId, data);
        }

        List<FramesTimeLineDto> frames = this.info.getFrames();

        for (FramesTimeLineDto frame : frames) {
            List<EventsTimeLineDto> events = frame.getEvents();

            for (EventsTimeLineDto event : events) {

                int participantId = event.getParticipantId();
                if(participantId == 0) continue;

                Map<String, List<SeqTypeData>> dataMap = result.get(participantId);

                // 리스트 세팅
                String type = event.getType();
                if(!dataMap.containsKey(type)) {
                    List<SeqTypeData> seqList = new ArrayList<>();
                    dataMap.put(type, seqList);
                }

                // 리스트 가져오기
                List<SeqTypeData> seqTypeData = dataMap.get(type);

                if(type.equalsIgnoreCase("ITEM_PURCHASED")) {
                    seqTypeData.add(new SeqTypeData(event, "ITEM_PURCHASED"));
                } else if(type.equalsIgnoreCase("SKILL_LEVEL_UP")) {
                    seqTypeData.add(new SeqTypeData(event, "SKILL_LEVEL_UP"));
                }

            }

        }

        return result;
    }


}
