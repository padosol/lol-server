package com.example.lolserver.web.repository;

import com.example.lolserver.entity.match.Match;
import com.example.lolserver.entity.match.MatchSummoner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MatchSummonerRepository extends JpaRepository<MatchSummoner, Long> {

    Optional<MatchSummoner> findMatchSummonerByMatchAndSummonerId(Match match, String summonerId);

    List<MatchSummoner> findMatchSummonerByPuuid(String puuid);

    List<MatchSummoner> findMatchSummonerByMatch(Match match);

    Page<MatchSummoner> findAllByPuuid(String puuid, Pageable pageable);

}
