package com.example.lolserver.repository.match.matchsummoner;

import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
import com.example.lolserver.repository.match.entity.id.MatchSummonerId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchSummonerRepository extends JpaRepository<MatchSummonerEntity, MatchSummonerId> {

}
