package com.example.lolserver.web.repository;

import com.example.lolserver.entity.match.Match;
import com.example.lolserver.entity.match.MatchSummoner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchSummonerRepository extends JpaRepository<MatchSummoner, Long> {

    Optional<MatchSummoner> findMatchSummonerByMatchAndSummonerId(Match match, String summonerId);

}
