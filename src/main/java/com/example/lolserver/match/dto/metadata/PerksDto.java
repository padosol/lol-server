package com.example.lolserver.match.dto.metadata;

import lombok.Data;

import java.util.List;

@Data
public class PerksDto {

    private PerkStatsDto statPerks;
    private List<PerkStyleDto> styles;

}
