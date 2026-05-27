package com.example.lolserver.gamedata.application.port.in;

import com.example.lolserver.gamedata.application.model.readmodel.VersionReadModel;

import java.util.List;

public interface VersionQueryUseCase {

    VersionReadModel getLatestVersion();

    List<VersionReadModel> getAllVersions();

    VersionReadModel getVersionById(Long versionId);
}
