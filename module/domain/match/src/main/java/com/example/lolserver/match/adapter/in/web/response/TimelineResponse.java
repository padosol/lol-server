package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.application.model.TimelineReadModel;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 매치 타임라인 API 응답 (참가자ID -> 타임라인).
 */
public record TimelineResponse(
        Map<Integer, ParticipantTimelineResponse> participants
) {
    public static TimelineResponse from(TimelineReadModel model) {
        Map<Integer, ParticipantTimelineResponse> participants = new LinkedHashMap<>();
        model.participants().forEach((participantId, timeline) ->
                participants.put(participantId, ParticipantTimelineResponse.from(timeline)));
        return new TimelineResponse(participants);
    }
}
