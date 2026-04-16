package com.example.lolserver.repository.match.timeline;

import com.example.lolserver.repository.match.dto.ItemEventDTO;
import com.example.lolserver.repository.match.dto.SkillEventDTO;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEventsEntity;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEventsEntity;

import java.util.List;

public interface TimelineRepositoryCustom {

    List<ItemEventsEntity> selectAllItemEventsByMatch(String matchId);

    List<SkillEventsEntity> selectAllSkillEventsByMatch(String matchId);

    List<ItemEventDTO> selectItemEventsByMatchIds(List<String> matchIds);

    List<SkillEventDTO> selectSkillEventsByMatchIds(List<String> matchIds);

}
