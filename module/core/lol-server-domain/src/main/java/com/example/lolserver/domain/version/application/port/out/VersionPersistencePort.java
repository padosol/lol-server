package com.example.lolserver.domain.version.application.port.out;

import com.example.lolserver.domain.version.application.model.VersionReadModel;

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
     * 전체 버전 목록을 조회합니다.
     * 최신 버전이 먼저 오도록 정렬됩니다.
     *
     * @return 버전 목록
     */
    List<VersionReadModel> findAllVersions();

    /**
     * ID로 버전을 조회합니다.
     *
     * @param versionId 버전 ID
     * @return 해당 버전 또는 empty
     */
    Optional<VersionReadModel> findById(Long versionId);
}
