package com.example.lolserver.esports.adapter.out.persistence.repository;

import com.example.lolserver.esports.adapter.out.persistence.entity.PersonSummonerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PersonSummonerJpaRepository extends JpaRepository<PersonSummonerEntity, String> {

    List<PersonSummonerEntity> findByPersonIdOrderByMainDescCreatedAtAsc(String personId);

    boolean existsByPersonId(String personId);

    /** 대표 계정은 인물당 하나(uk_person_summoner_main) — 새 대표를 저장하기 전에 나머지를 먼저 해제한다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PersonSummonerEntity s SET s.main = false, s.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE s.personId = :personId AND s.main = true AND s.puuid <> :puuid")
    int unsetMainExcept(@Param("personId") String personId, @Param("puuid") String puuid);
}
