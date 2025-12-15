package com.example.lolserver.repository.match.match.dsl;

import com.example.lolserver.repository.match.entity.MatchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MatchRepositoryCustom {

    Page<MatchEntity> getMatches(String puuid, Integer queueId, Pageable pageable);

}
