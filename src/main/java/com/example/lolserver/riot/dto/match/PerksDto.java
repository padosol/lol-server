package com.example.lolserver.riot.dto.match;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerksDto {

    private PerkStatsDto statPerks;
    private List<PerkStyleDto> styles;

}
