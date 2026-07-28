package com.example.lolserver.match.application.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참가자 룬 스탯 읽기 모델. 필드명은 캐시/API 계약이므로 변경 금지.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatValueReadModel {

    private int defense;
    private int flex;
    private int offense;
}
