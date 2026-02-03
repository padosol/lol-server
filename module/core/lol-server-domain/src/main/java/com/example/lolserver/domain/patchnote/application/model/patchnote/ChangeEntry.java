package com.example.lolserver.domain.patchnote.application.model.patchnote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChangeEntry(
    String targetName,
    String type,
    String direction,
    List<StatChange> changes
) {}
