package com.example.lolserver.match.application.model;

import com.example.lolserver.match.domain.gamedata.timeline.ItemSeqData;

/**
 * 타임라인 아이템 구매 시퀀스 읽기 모델.
 */
public record ItemSeqReadModel(
        int itemId,
        long minute,
        String type
) {
    public static ItemSeqReadModel of(ItemSeqData data) {
        return new ItemSeqReadModel(data.getItemId(), data.getMinute(), data.getType());
    }
}
