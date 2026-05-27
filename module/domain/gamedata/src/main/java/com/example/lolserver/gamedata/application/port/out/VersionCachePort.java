package com.example.lolserver.gamedata.application.port.out;

import com.example.lolserver.gamedata.application.model.readmodel.VersionReadModel;

/**
 * Version 캐시 포트 - Redis 어댑터에서 구현
 */
public interface VersionCachePort {

    /**
     * 캐시된 최신 버전을 조회합니다.
     *
     * @return 캐시된 최신 버전 또는 null
     */
    VersionReadModel findLatestVersion();

    /**
     * 최신 버전을 캐시에 저장합니다.
     *
     * @param versionReadModel 저장할 버전 정보
     */
    void saveLatestVersion(VersionReadModel versionReadModel);
}
