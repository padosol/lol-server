package com.example.lolserver.domain.patchnote.application.model;

public record PatchNoteSummaryReadModel(
    String versionId,
    String title,
    String createdAt
) {}
