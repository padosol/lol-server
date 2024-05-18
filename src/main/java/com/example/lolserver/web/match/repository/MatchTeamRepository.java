package com.example.lolserver.web.match.repository;

import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.entity.MatchTeam;
import com.example.lolserver.web.match.entity.id.MatchTeamId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchTeamRepository extends JpaRepository<MatchTeam, MatchTeamId> {

}
