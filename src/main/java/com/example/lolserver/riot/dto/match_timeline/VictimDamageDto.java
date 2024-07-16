package com.example.lolserver.riot.dto.match_timeline;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VictimDamageDto {
    boolean basic;
    int magicDamage;
    String name;
    int participantId;
    int physicalDamage;
    String spellName;
    int spellSlot;
    int trueDamage;
    String type;
}
