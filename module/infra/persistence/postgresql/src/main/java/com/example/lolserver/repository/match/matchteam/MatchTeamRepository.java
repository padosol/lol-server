package com.example.lolserver.repository.match.matchteam;

import com.example.lolserver.repository.match.entity.MatchTeamEntity;
import com.example.lolserver.repository.match.entity.id.MatchTeamId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface MatchTeamRepository extends JpaRepository<MatchTeamEntity, MatchTeamId> {

    List<MatchTeamEntity> findByMatchId(String matchId);
}
