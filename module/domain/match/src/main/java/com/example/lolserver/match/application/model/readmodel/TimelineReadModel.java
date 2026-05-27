package com.example.lolserver.match.application.model.readmodel;

import java.util.Map;

/**
 * 매치 타임라인 읽기 모델 (참가자ID -> 타임라인). 영속 어댑터가 직접 빌드한다.
 */
public record TimelineReadModel(
        Map<Integer, ParticipantTimelineReadModel> participants
) {
}
