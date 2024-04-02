package com.example.lolserver.web.match.repository;

import com.example.lolserver.web.match.entity.MatchTeam;
import com.example.lolserver.web.match.entity.MatchTeamBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchTeamBanRepository extends JpaRepository<MatchTeamBan, Long> {

    List<MatchTeamBan> findMatchTeamBansByMatchTeam(MatchTeam matchTeam);

}
