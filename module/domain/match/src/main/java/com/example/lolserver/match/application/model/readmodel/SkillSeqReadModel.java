package com.example.lolserver.match.application.model.readmodel;

/**
 * 타임라인 스킬 레벨업 시퀀스 읽기 모델. 영속 어댑터가 직접 빌드한다.
 */
public record SkillSeqReadModel(
        int skillSlot,
        long minute,
        String type
) {
}
