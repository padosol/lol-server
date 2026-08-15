package com.example.lolserver.gamedata.adapter.out.persistence;

import com.example.lolserver.gamedata.adapter.out.persistence.entity.VersionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VersionJpaRepository extends JpaRepository<VersionEntity, Long> {

    @Query("SELECT v FROM VersionEntity v ORDER BY v.versionId DESC LIMIT 1")
    Optional<VersionEntity> findLatestVersion();

    /**
     * 최신 버전부터 {@code pageable} 크기만큼 조회한다.
     * 몇 개를 노출할지는 정책이라 애플리케이션 계층이 정한다.
     */
    @Query("SELECT v FROM VersionEntity v ORDER BY v.versionId DESC")
    List<VersionEntity> findRecentVersions(Pageable pageable);
}
