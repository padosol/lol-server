package com.example.lolserver.match.adapter.out.persistence.timeline;

import com.example.lolserver.match.adapter.out.persistence.dto.TimelineEventDTO;

import java.util.List;

public interface TimelineRepositoryCustom {

    List<TimelineEventDTO> selectAllTimelineEventsByMatch(String matchId);

    List<TimelineEventDTO> selectTimelineEventsByMatchIds(List<String> matchIds);

}
