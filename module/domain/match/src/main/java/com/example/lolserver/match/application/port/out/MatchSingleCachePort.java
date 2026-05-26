package com.example.lolserver.match.application.port.out;

import com.example.lolserver.match.application.model.GameReadModel;

import java.util.Collection;
import java.util.Map;

public interface MatchSingleCachePort {

    /**
     * Redis MGET match:v1:{matchId} 로 단건 매치 캐시를 일괄 조회한다.
     * 누락된 matchId 는 결과 맵에 포함하지 않는다.
     */
    Map<String, GameReadModel> findByIds(Collection<String> matchIds);
}
