package com.example.lolserver.repository.match.matchsummoner;

import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchSummonerRepository extends JpaRepository<MatchSummonerEntity, Long> {

    List<MatchSummonerEntity> findByMatchId(String matchId);

    List<MatchSummonerEntity> findByMatchIdIn(List<String> matchIds);

}
