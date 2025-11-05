package com.example.lolserver.storage.db.core.repository.match.matchsummoner;

import com.example.lolserver.storage.db.core.repository.match.entity.MatchSummoner;
import com.example.lolserver.storage.db.core.repository.match.entity.id.MatchSummonerId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchSummonerRepository extends JpaRepository<MatchSummoner, MatchSummonerId> {

}
