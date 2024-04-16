package com.example.lolserver.riot.dto.spectator;

import lombok.Getter;

import java.util.List;

@Getter
public class Perks {

    private List<Long> perkIds;
    private	long perkStyle;
    private	long perkSubStyle;
}
