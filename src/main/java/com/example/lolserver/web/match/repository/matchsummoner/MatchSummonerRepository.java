package com.example.lolserver.web.match.repository.matchsummoner;

import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.web.match.entity.id.MatchSummonerId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchSummonerRepository extends JpaRepository<MatchSummoner, MatchSummonerId> {

}
