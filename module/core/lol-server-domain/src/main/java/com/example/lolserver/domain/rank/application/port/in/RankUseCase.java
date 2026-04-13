package com.example.lolserver.domain.rank.application.port.in;

import com.example.lolserver.domain.rank.application.model.RankReadModel;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import com.example.lolserver.support.PageResult;

public interface RankUseCase {
    PageResult<RankReadModel> getRanks(RankSearchDto rankSearchDto, String platformId);
}
