package com.example.lolserver.match.adapter.out.persistence.matchsummoner;

import com.example.lolserver.match.adapter.out.persistence.entity.MatchSummonerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchSummonerRepository extends JpaRepository<MatchSummonerEntity, Long> {

    List<MatchSummonerEntity> findByMatchId(String matchId);

    List<MatchSummonerEntity> findByMatchIdIn(List<String> matchIds);

}
