package com.example.lolserver.gamedata.application;

import com.example.lolserver.gamedata.application.model.readmodel.VersionReadModel;
import com.example.lolserver.gamedata.application.port.in.VersionQueryUseCase;
import com.example.lolserver.gamedata.application.port.out.VersionPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VersionService implements VersionQueryUseCase {

    /**
     * 버전 목록 노출 상한. 화면에서 고르는 대상은 현재 패치와 직전 패치뿐이라
     * 시즌이 쌓여도 목록이 길어지지 않도록 여기서 자른다.
     */
    private static final int RECENT_VERSION_LIMIT = 2;

    private final VersionFinder versionFinder;
    private final VersionPersistencePort versionPersistencePort;

    public VersionReadModel getLatestVersion() {
        return versionFinder.findLatestVersion();
    }

    public List<VersionReadModel> getRecentVersions() {
        return versionPersistencePort.findRecentVersions(RECENT_VERSION_LIMIT);
    }

    public VersionReadModel getVersionById(Long versionId) {
        return versionPersistencePort.findById(versionId).orElse(null);
    }
}
