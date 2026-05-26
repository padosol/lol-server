package com.example.lolserver.match.application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 참가자 룬 스타일 읽기 모델. 필드명은 캐시/API 계약이므로 변경 금지.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StyleReadModel {

    private int primaryStyleId;
    private int primaryPerk0;
    private int primaryPerk1;
    private int primaryPerk2;
    private int primaryPerk3;

    private int subStyleId;
    private int subPerk0;
    private int subPerk1;
}
