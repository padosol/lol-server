package com.example.lolserver.match.application.model;

/**
 * 타임라인 아이템 구매 시퀀스 읽기 모델. 영속 어댑터가 직접 빌드한다.
 */
public record ItemSeqReadModel(
        int itemId,
        long minute,
        String type
) {
}
