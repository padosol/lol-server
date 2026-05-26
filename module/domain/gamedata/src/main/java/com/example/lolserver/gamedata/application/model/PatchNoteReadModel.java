package com.example.lolserver.gamedata.application.model;

import com.example.lolserver.gamedata.application.model.patchnote.PatchNoteContent;

public record PatchNoteReadModel(
    String versionId,
    String title,
    PatchNoteContent content,
    String patchUrl,
    String createdAt
) {}
