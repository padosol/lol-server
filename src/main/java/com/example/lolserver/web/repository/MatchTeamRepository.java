package com.example.lolserver.web.repository;

import com.example.lolserver.entity.match.Match;
import com.example.lolserver.entity.match.MatchTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchTeamRepository extends JpaRepository<MatchTeam, Long> {

    Optional<MatchTeam> findMatchTeamByMatchAndTeamId(Match match, int teamId);
    List<MatchTeam> findMatchTeamsByMatch(Match match);
}
