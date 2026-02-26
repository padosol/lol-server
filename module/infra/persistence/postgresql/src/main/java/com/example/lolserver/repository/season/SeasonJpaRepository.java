package com.example.lolserver.repository.season;

import com.example.lolserver.repository.season.entity.SeasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonJpaRepository extends JpaRepository<SeasonEntity, Long> {

    @Query("SELECT DISTINCT s FROM SeasonEntity s LEFT JOIN FETCH s.patchVersions ORDER BY s.seasonValue DESC")
    List<SeasonEntity> findAllWithPatchVersions();

    @Query("SELECT s FROM SeasonEntity s LEFT JOIN FETCH s.patchVersions WHERE s.seasonId = :seasonId")
    Optional<SeasonEntity> findByIdWithPatchVersions(@Param("seasonId") Long seasonId);
}
