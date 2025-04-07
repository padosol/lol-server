package com.example.lolserver.web.match.repository.matchteam;

import com.example.lolserver.web.match.entity.MatchTeam;
import com.example.lolserver.web.match.entity.id.MatchTeamId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface MatchTeamRepository extends JpaRepository<MatchTeam, MatchTeamId> {

}
