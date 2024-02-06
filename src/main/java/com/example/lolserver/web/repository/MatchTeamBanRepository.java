package com.example.lolserver.web.repository;

import com.example.lolserver.entity.match.MatchTeam;
import com.example.lolserver.entity.match.MatchTeamBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchTeamBanRepository extends JpaRepository<MatchTeamBan, Long> {

    Optional<MatchTeamBan> findMatchTeamBanByMatchTeam(MatchTeam matchTeam);

}
