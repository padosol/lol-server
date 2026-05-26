package com.example.lolserver.match.adapter.out.persistence.match;

import com.example.lolserver.match.adapter.out.persistence.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

    Optional<MatchEntity> findByMatchId(String matchId);

    List<MatchEntity> findAllByMatchIdIn(Collection<String> matchIds);
}
