package com.example.lolserver.web.match.repository.match.dsl;

import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.entity.Match;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MatchRepositoryCustom {

    Page<Match> getMatches(MatchRequest matchRequest, Pageable pageable);

    List<Match> getAllMatches();

    List<String> getMatchIdsNotIn(List<String> matchId);
}
