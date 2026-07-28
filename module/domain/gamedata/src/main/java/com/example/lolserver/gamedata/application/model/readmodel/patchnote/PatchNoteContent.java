package com.example.lolserver.gamedata.application.model.readmodel.patchnote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PatchNoteContent(
    String version,
    GameModeChanges rift,
    GameModeChanges arena,
    AramChanges aram
) {}
