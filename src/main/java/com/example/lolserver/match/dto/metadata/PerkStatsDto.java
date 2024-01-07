package com.example.lolserver.match.dto.metadata;

import lombok.Data;

import java.util.List;

@Data
public class PerkStatsDto {

    private String description;
    private List<PerkStyleSelectionDto> selections;
    private int style;

}
