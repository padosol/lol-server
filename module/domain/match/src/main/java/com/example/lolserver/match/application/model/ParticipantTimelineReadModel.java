package com.example.lolserver.match.application.model;

import com.example.lolserver.match.domain.gamedata.timeline.ParticipantTimeline;

import java.util.List;

/**
 * 참가자 1명의 타임라인(아이템/스킬 시퀀스) 읽기 모델.
 */
public record ParticipantTimelineReadModel(
        List<ItemSeqReadModel> itemSeq,
        List<SkillSeqReadModel> skillSeq
) {
    public static ParticipantTimelineReadModel of(ParticipantTimeline timeline) {
        return new ParticipantTimelineReadModel(
                timeline.getItemSeq().stream().map(ItemSeqReadModel::of).toList(),
                timeline.getSkillSeq().stream().map(SkillSeqReadModel::of).toList()
        );
    }
}
