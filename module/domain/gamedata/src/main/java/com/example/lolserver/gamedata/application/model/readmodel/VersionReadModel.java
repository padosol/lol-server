package com.example.lolserver.gamedata.application.model.readmodel;

import java.time.LocalDateTime;

/**
 * 버전 정보 ReadModel
 */
public record VersionReadModel(
    Long versionId,
    String versionValue,
    LocalDateTime createdAt
) {}
