package com.example.lolserver.gamedata.application.model.readmodel;

import com.example.lolserver.gamedata.application.model.readmodel.patchnote.PatchNoteContent;

public record PatchNoteReadModel(
    String versionId,
    String title,
    PatchNoteContent content,
    String patchUrl,
    String createdAt
) {}
