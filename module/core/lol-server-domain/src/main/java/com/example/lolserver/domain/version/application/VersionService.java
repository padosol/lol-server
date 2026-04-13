package com.example.lolserver.domain.version.application;

import com.example.lolserver.domain.version.application.model.VersionReadModel;
import com.example.lolserver.domain.version.application.port.out.VersionPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VersionService {

    private final VersionFinder versionFinder;
    private final VersionPersistencePort versionPersistencePort;

    public VersionReadModel getLatestVersion() {
        return versionFinder.findLatestVersion();
    }

    public List<VersionReadModel> getAllVersions() {
        return versionPersistencePort.findAllVersions();
    }

    public VersionReadModel getVersionById(Long versionId) {
        return versionPersistencePort.findById(versionId).orElse(null);
    }
}
