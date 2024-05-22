package com.example.lolserver.web.match.repository.match;

import com.example.lolserver.web.match.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, String> {


    @Query("select m from Match m join fetch MatchSummoner ms on m.matchId = ms.match.matchId")
    List<Match> findAllByTest();


}
