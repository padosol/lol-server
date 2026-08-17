package com.example.lolserver.gamedata.application.port.in;

import com.example.lolserver.gamedata.application.model.readmodel.VersionReadModel;

import java.util.List;

public interface VersionQueryUseCase {

    VersionReadModel getLatestVersion();

    /**
     * 최신 패치부터 노출 상한만큼의 버전 목록을 반환합니다.
     */
    List<VersionReadModel> getRecentVersions();

    VersionReadModel getVersionById(Long versionId);
}
