package com.example.lolserver.storage.db.core.repository.match.match.dsl;

import com.example.lolserver.storage.db.core.repository.match.entity.Match;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MatchRepositoryCustom {

    Page<Match> getMatches(String puuid, Integer queueId, Pageable pageable);

}
