package com.example.lolserver.gamedata.application.model.readmodel;

import java.time.LocalDateTime;

/**
 * 버전 정보 ReadModel
 *
 * @param versionValue     패치 버전 (예: 16.16)
 * @param patchVersionData Data Dragon 정적 데이터 버전 (예: 16.16.1).
 *                         챔피언 아이콘 같은 정적 리소스 URL 은 이 값으로 만든다.
 *                         아직 채워지지 않은 과거 패치는 null.
 */
public record VersionReadModel(
    Long versionId,
    String versionValue,
    String patchVersionData,
    LocalDateTime createdAt
) {}
