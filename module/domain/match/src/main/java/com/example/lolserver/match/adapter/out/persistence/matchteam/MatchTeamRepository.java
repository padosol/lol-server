package com.example.lolserver.match.adapter.out.persistence.matchteam;

import com.example.lolserver.match.adapter.out.persistence.entity.MatchTeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface MatchTeamRepository extends JpaRepository<MatchTeamEntity, Long> {

    List<MatchTeamEntity> findByMatchId(String matchId);

    List<MatchTeamEntity> findByMatchIdIn(List<String> matchIds);
}
