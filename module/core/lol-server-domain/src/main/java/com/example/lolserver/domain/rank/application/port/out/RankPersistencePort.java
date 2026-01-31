package com.example.lolserver.domain.rank.application.port.out;

import com.example.lolserver.domain.rank.domain.Rank;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import org.springframework.data.domain.Page;

public interface RankPersistencePort {
    Page<Rank> getRanks(RankSearchDto rankSearchDto);
}
