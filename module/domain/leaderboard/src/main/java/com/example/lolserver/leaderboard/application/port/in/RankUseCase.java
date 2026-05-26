package com.example.lolserver.leaderboard.application.port.in;

import com.example.lolserver.leaderboard.application.model.RankReadModel;
import com.example.lolserver.leaderboard.application.dto.RankSearchDto;
import com.example.lolserver.common.support.PageResult;

public interface RankUseCase {
    PageResult<RankReadModel> getRanks(RankSearchDto rankSearchDto, String platformId);
}
