package com.example.lolserver.storage.db.core.repository.match.timeline;


import com.example.lolserver.storage.db.core.repository.match.entity.timeline.TimeLineEvent;
import com.example.lolserver.storage.db.core.repository.match.entity.timeline.events.ItemEvents;
import com.example.lolserver.storage.db.core.repository.match.entity.timeline.events.SkillEvents;

import java.util.List;

public interface TimelineRepositoryCustom {

    List<TimeLineEvent> selectAllTimelineInfo(String matchId);

    List<ItemEvents> selectAllItemEventsByMatch(String matchId);

    List<SkillEvents> selectAllSkillEventsByMatch(String matchId);

}
