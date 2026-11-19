package com.example.lolserver.esports.adapter.out.persistence.repository;

import com.example.lolserver.esports.adapter.out.persistence.entity.EsportsContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EsportsContractJpaRepository extends JpaRepository<EsportsContractEntity, Long> {

    List<EsportsContractEntity> findByPersonIdOrderByStartDateAscContractIdAsc(String personId);

    boolean existsByPersonId(String personId);

    /** 마지막 날(조기 종료일 또는 만료일)이 {@code date} 이후(포함)이거나 끝이 없는 계약이 있는가. */
    @Query("SELECT COUNT(c) > 0 FROM EsportsContractEntity c WHERE c.teamId = :teamId "
            + "AND (COALESCE(c.actualEndDate, c.endDate) IS NULL OR COALESCE(c.actualEndDate, c.endDate) >= :date)")
    boolean existsEffectiveOnOrAfter(@Param("teamId") String teamId, @Param("date") LocalDate date);
}
