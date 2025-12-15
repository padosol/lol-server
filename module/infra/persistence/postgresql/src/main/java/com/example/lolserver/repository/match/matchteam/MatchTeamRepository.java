package com.example.lolserver.repository.match.matchteam;

import com.example.lolserver.repository.match.entity.MatchTeamEntity;
import com.example.lolserver.repository.match.entity.id.MatchTeamId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface MatchTeamRepository extends JpaRepository<MatchTeamEntity, MatchTeamId> {

}
