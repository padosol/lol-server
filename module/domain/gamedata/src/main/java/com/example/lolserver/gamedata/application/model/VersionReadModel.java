package com.example.lolserver.gamedata.application.model;

import java.time.LocalDateTime;

/**
 * 버전 정보 ReadModel
 */
public record VersionReadModel(
    Long versionId,
    String versionValue,
    LocalDateTime createdAt
) {}
