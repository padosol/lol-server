package com.example.lolserver.leaderboard.application.port.out;

import com.example.lolserver.leaderboard.domain.Rank;
import com.example.lolserver.leaderboard.application.dto.RankSearchDto;
import com.example.lolserver.common.support.PageResult;

public interface RankPersistencePort {
    PageResult<Rank> getRanks(RankSearchDto rankSearchDto, String platformId);
}
