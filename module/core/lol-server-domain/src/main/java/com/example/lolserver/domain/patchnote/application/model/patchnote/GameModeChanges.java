package com.example.lolserver.domain.patchnote.application.model.patchnote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GameModeChanges(
    List<ChangeEntry> champions,
    List<ChangeEntry> items,
    List<ChangeEntry> systems
) {}
