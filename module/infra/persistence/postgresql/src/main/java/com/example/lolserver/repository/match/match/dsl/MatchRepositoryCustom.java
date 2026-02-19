package com.example.lolserver.repository.match.match.dsl;

import com.example.lolserver.repository.match.entity.MatchEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface MatchRepositoryCustom {

    Slice<MatchEntity> getMatches(String puuid, Integer queueId, Pageable pageable);

}
