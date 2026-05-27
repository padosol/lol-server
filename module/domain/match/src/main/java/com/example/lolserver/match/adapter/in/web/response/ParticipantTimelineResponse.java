package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.application.model.readmodel.ParticipantTimelineReadModel;

import java.util.List;

/**
 * 타임라인 API 응답 - 참가자 1명의 아이템/스킬 시퀀스.
 */
public record ParticipantTimelineResponse(
        List<ItemSeqResponse> itemSeq,
        List<SkillSeqResponse> skillSeq
) {
    public static ParticipantTimelineResponse from(ParticipantTimelineReadModel model) {
        return new ParticipantTimelineResponse(
                model.itemSeq().stream().map(item -> ItemSeqResponse.from(item)).toList(),
                model.skillSeq().stream().map(skill -> SkillSeqResponse.from(skill)).toList()
        );
    }
}
