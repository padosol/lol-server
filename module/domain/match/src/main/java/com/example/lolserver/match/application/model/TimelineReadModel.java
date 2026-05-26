package com.example.lolserver.match.application.model;

import com.example.lolserver.match.domain.TimelineData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 매치 타임라인 읽기 모델 (참가자ID -> 타임라인).
 */
public record TimelineReadModel(
        Map<Integer, ParticipantTimelineReadModel> participants
) {
    public static TimelineReadModel of(TimelineData data) {
        Map<Integer, ParticipantTimelineReadModel> participants = new LinkedHashMap<>();
        data.getParticipants().forEach((participantId, timeline) ->
                participants.put(participantId, ParticipantTimelineReadModel.of(timeline)));
        return new TimelineReadModel(participants);
    }
}
