package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.application.model.SkillSeqReadModel;
import com.example.lolserver.match.domain.gamedata.timeline.SkillSeqData;

/**
 * 스킬 레벨업 시퀀스 API 응답. 타임라인/매치 참가자 양쪽에서 공유한다.
 */
public record SkillSeqResponse(
        int skillSlot,
        long minute,
        String type
) {
    public static SkillSeqResponse from(SkillSeqReadModel model) {
        return new SkillSeqResponse(model.skillSlot(), model.minute(), model.type());
    }

    public static SkillSeqResponse from(SkillSeqData data) {
        return new SkillSeqResponse(data.getSkillSlot(), data.getMinute(), data.getType());
    }
}
