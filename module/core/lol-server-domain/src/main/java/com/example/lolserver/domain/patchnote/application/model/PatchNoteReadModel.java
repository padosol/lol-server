package com.example.lolserver.domain.patchnote.application.model;

import com.example.lolserver.domain.patchnote.application.model.patchnote.PatchNoteContent;

public record PatchNoteReadModel(
    String versionId,
    String title,
    PatchNoteContent content,
    String patchUrl,
    String createdAt
) {}
