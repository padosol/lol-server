package com.example.lolserver.riot.dto.match;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerkStyleDto {

    private String description;
    private List<PerkStyleSelectionDto> selections;
    private int style;
}
