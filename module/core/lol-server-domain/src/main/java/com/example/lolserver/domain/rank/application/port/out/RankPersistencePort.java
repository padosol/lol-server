package com.example.lolserver.domain.rank.application.port.out;

import com.example.lolserver.domain.rank.domain.Rank;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;

import java.util.List;

public interface RankPersistencePort {
    List<Rank> getRanks(RankSearchDto rankSearchDto);
}
