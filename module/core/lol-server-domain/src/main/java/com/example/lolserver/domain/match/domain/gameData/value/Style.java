package com.example.lolserver.domain.match.domain.gameData.value;

import com.example.lolserver.repository.match.entity.value.matchsummoner.StyleValue;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class Style {

    private int primaryRuneId;
    private int[] primaryRuneIds;

    private int secondaryRuneId;
    private int[] secondaryRuneIds;

    public Style(){}

    public Style(StyleValue styleValue) {
        this.primaryRuneId = styleValue.getPrimaryRuneId();
        this.primaryRuneIds = Arrays.stream(styleValue.getPrimaryRuneIds().split(",")).mapToInt(Integer::valueOf).toArray();

        this.secondaryRuneId = styleValue.getSecondaryRuneId();
        this.secondaryRuneIds = Arrays.stream(styleValue.getSecondaryRuneIds().split(",")).mapToInt(Integer::valueOf).toArray();

    }
}
