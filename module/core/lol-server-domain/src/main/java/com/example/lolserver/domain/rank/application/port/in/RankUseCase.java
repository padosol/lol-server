package com.example.lolserver.domain.rank.application.port.in;

import com.example.lolserver.domain.rank.application.dto.RankResponse;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;

import java.util.List;

public interface RankUseCase {
    List<RankResponse> getRanks(RankSearchDto rankSearchDto);
}
