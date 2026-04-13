package com.example.lolserver.domain.version.application.port.in;

import com.example.lolserver.domain.version.application.model.VersionReadModel;

import java.util.List;

public interface VersionQueryUseCase {

    VersionReadModel getLatestVersion();

    List<VersionReadModel> getAllVersions();

    VersionReadModel getVersionById(Long versionId);
}
