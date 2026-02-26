package com.example.lolserver.domain.rank.application.port.in;

import com.example.lolserver.domain.rank.application.model.RankReadModel;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import org.springframework.data.domain.Page;

public interface RankUseCase {
    Page<RankReadModel> getRanks(RankSearchDto rankSearchDto, String platformId);
}
