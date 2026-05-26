package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.application.model.ItemSeqReadModel;
import com.example.lolserver.match.domain.gamedata.timeline.ItemSeqData;

/**
 * 아이템 구매 시퀀스 API 응답. 타임라인/매치 참가자 양쪽에서 공유한다.
 */
public record ItemSeqResponse(
        int itemId,
        long minute,
        String type
) {
    public static ItemSeqResponse from(ItemSeqReadModel model) {
        return new ItemSeqResponse(model.itemId(), model.minute(), model.type());
    }

    public static ItemSeqResponse from(ItemSeqData data) {
        return new ItemSeqResponse(data.getItemId(), data.getMinute(), data.getType());
    }
}
