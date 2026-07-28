package com.example.lolserver.match.application.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참가자 아이템 슬롯 읽기 모델. 필드명은 캐시/API 계약이므로 변경 금지.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemValueReadModel {

    private int item0;
    private int item1;
    private int item2;
    private int item3;
    private int item4;
    private int item5;
    private int item6;
}
