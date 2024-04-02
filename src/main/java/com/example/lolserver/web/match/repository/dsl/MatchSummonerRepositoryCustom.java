package com.example.lolserver.web.match.repository.dsl;

import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.web.dto.request.MatchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MatchSummonerRepositoryCustom {

    Page<MatchSummoner> findAllByPuuidAndQueueId(MatchRequest matchRequest, Pageable pageable);


}
