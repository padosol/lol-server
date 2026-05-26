package com.example.lolserver.gamedata.application.model.patchnote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StatChange(
    String statName,
    String before,
    String after
) {}
