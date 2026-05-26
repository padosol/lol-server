package com.example.lolserver.gamedata.application;

import com.example.lolserver.gamedata.application.model.VersionReadModel;
import com.example.lolserver.gamedata.application.port.out.VersionCachePort;
import com.example.lolserver.gamedata.application.port.out.VersionPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VersionFinder {

    private final VersionCachePort versionCachePort;
    private final VersionPersistencePort versionPersistencePort;

    public VersionReadModel findLatestVersion() {
        return Optional.ofNullable(versionCachePort.findLatestVersion())
                .orElseGet(() -> {
                    Optional<VersionReadModel> dbVersion = versionPersistencePort.findLatestVersion();
                    dbVersion.ifPresent(versionCachePort::saveLatestVersion);
                    return dbVersion.orElse(null);
                });
    }
}
