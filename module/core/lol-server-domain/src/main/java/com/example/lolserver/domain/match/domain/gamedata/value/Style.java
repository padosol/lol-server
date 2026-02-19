package com.example.lolserver.domain.match.domain.gamedata.value;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Style {

    private int primaryRuneId;
    private int[] primaryRuneIds;

    private int secondaryRuneId;
    private int[] secondaryRuneIds;

}
