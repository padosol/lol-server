package com.example.lolserver.web.match.repository.timeline;

import com.example.lolserver.web.match.entity.timeline.TimeLineEvent;
import com.example.lolserver.web.match.entity.timeline.events.ItemEvents;
import com.example.lolserver.web.match.entity.timeline.events.SkillEvents;

import java.util.List;

public interface TimelineRepositoryCustom {

    List<TimeLineEvent> selectAllTimelineInfo(String matchId);

    List<ItemEvents> selectAllItemEventsByMatch(String matchId);

    List<SkillEvents> selectAllSkillEventsByMatch(String matchId);

}
