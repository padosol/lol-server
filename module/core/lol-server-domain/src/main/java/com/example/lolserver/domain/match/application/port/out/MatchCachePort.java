package com.example.lolserver.domain.match.application.port.out;

import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.support.SliceResult;

public interface MatchCachePort {

    SliceResult<GameReadModel> findMatchesBatch(String puuid, Integer season, Integer queueId, Integer pageNo);

    void saveMatchesBatch(
            String puuid, Integer season, Integer queueId, Integer pageNo,
            SliceResult<GameReadModel> matches);
}
