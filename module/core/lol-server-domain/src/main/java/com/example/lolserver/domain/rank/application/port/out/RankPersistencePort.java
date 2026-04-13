package com.example.lolserver.domain.rank.application.port.out;

import com.example.lolserver.domain.rank.domain.Rank;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import com.example.lolserver.support.PageResult;

public interface RankPersistencePort {
    PageResult<Rank> getRanks(RankSearchDto rankSearchDto, String platformId);
}
