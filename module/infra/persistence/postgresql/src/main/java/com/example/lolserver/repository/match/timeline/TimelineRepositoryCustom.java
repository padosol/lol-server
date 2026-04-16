package com.example.lolserver.repository.match.timeline;

import com.example.lolserver.repository.match.dto.TimelineEventDTO;

import java.util.List;

public interface TimelineRepositoryCustom {

    List<TimelineEventDTO> selectAllTimelineEventsByMatch(String matchId);

    List<TimelineEventDTO> selectTimelineEventsByMatchIds(List<String> matchIds);

}
