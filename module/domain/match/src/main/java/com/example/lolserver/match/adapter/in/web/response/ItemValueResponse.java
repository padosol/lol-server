package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.domain.gamedata.value.ItemValue;

/**
 * 매치 API 응답 - 참가자 아이템 슬롯.
 */
public record ItemValueResponse(
        int item0,
        int item1,
        int item2,
        int item3,
        int item4,
        int item5,
        int item6
) {
    public static ItemValueResponse from(ItemValue value) {
        return new ItemValueResponse(
                value.getItem0(),
                value.getItem1(),
                value.getItem2(),
                value.getItem3(),
                value.getItem4(),
                value.getItem5(),
                value.getItem6()
        );
    }
}
