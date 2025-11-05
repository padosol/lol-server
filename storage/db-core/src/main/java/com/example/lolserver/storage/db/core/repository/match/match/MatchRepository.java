package com.example.lolserver.storage.db.core.repository.match.match;

import com.example.lolserver.storage.db.core.repository.match.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRepository extends JpaRepository<Match, String> {
}
