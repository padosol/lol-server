package com.example.lolserver.repository.match.timeline;



import com.example.lolserver.repository.match.dto.ItemEventDTO;
import com.example.lolserver.repository.match.dto.SkillEventDTO;
import com.example.lolserver.repository.match.entity.timeline.TimeLineEventEntity;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEventsEntity;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEventsEntity;

import java.util.List;

public interface TimelineRepositoryCustom {

    List<TimeLineEventEntity> selectAllTimelineInfo(String matchId);

    List<ItemEventsEntity> selectAllItemEventsByMatch(String matchId);

    List<SkillEventsEntity> selectAllSkillEventsByMatch(String matchId);

    List<ItemEventsEntity> selectAllItemEventsByMatchIds(List<String> matchIds);

    List<SkillEventsEntity> selectAllSkillEventsByMatchIds(List<String> matchIds);

    List<ItemEventDTO> selectItemEventsByMatchIds(List<String> matchIds);

    List<SkillEventDTO> selectSkillEventsByMatchIds(List<String> matchIds);

}
