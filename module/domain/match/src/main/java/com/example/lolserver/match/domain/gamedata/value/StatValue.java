package com.example.lolserver.match.domain.gamedata.value;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatValue {

    private int defense;
    private int flex;
    private int offense;

}
