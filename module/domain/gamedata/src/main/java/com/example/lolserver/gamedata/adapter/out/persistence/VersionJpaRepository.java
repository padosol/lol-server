package com.example.lolserver.gamedata.adapter.out.persistence;

import com.example.lolserver.gamedata.adapter.out.persistence.entity.VersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VersionJpaRepository extends JpaRepository<VersionEntity, Long> {

    @Query("SELECT v FROM VersionEntity v ORDER BY v.versionId DESC LIMIT 1")
    Optional<VersionEntity> findLatestVersion();

    @Query("SELECT v FROM VersionEntity v ORDER BY v.versionId DESC")
    List<VersionEntity> findAllOrderByVersionIdDesc();
}
