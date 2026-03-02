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

    private int primaryStyleId;
    private int primaryPerk0;
    private int primaryPerk1;
    private int primaryPerk2;
    private int primaryPerk3;

    private int subStyleId;
    private int subPerk0;
    private int subPerk1;

}
