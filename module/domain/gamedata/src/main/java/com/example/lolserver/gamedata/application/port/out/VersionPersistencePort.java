package com.example.lolserver.gamedata.application.port.out;

import com.example.lolserver.gamedata.application.model.readmodel.VersionReadModel;

import java.util.List;
import java.util.Optional;

/**
 * Version 영속성 포트 - PostgreSQL 어댑터에서 구현
 */
public interface VersionPersistencePort {

    /**
     * 최신 버전을 조회합니다.
     *
     * @return 최신 버전 또는 empty
     */
    Optional<VersionReadModel> findLatestVersion();

    /**
     * 최근 버전 목록을 조회합니다.
     * 최신 버전이 먼저 오도록 정렬됩니다.
     *
     * @param limit 조회할 최대 개수
     * @return 버전 목록 (최대 {@code limit} 개)
     */
    List<VersionReadModel> findRecentVersions(int limit);

    /**
     * ID로 버전을 조회합니다.
     *
     * @param versionId 버전 ID
     * @return 해당 버전 또는 empty
     */
    Optional<VersionReadModel> findById(Long versionId);
}
