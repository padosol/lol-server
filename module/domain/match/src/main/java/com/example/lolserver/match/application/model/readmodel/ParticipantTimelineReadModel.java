package com.example.lolserver.match.application.model.readmodel;

import java.util.List;

/**
 * 참가자 1명의 타임라인(아이템/스킬 시퀀스) 읽기 모델. 영속 어댑터가 직접 빌드한다.
 */
public record ParticipantTimelineReadModel(
        List<ItemSeqReadModel> itemSeq,
        List<SkillSeqReadModel> skillSeq
) {
}
