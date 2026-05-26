package com.example.lolserver.match.application.model;

import com.example.lolserver.match.domain.gamedata.timeline.SkillSeqData;

/**
 * 타임라인 스킬 레벨업 시퀀스 읽기 모델.
 */
public record SkillSeqReadModel(
        int skillSlot,
        long minute,
        String type
) {
    public static SkillSeqReadModel of(SkillSeqData data) {
        return new SkillSeqReadModel(data.getSkillSlot(), data.getMinute(), data.getType());
    }
}
