package com.example.lolserver.storage.db.core.repository.match.matchteam;

import com.example.lolserver.storage.db.core.repository.match.entity.MatchTeam;
import com.example.lolserver.storage.db.core.repository.match.entity.id.MatchTeamId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface MatchTeamRepository extends JpaRepository<MatchTeam, MatchTeamId> {

}
