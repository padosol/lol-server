package com.example.lolserver.domain.rank.application;

import com.example.lolserver.domain.rank.application.port.in.RankUseCase;
import com.example.lolserver.domain.rank.application.port.out.RankPersistencePort;
import com.example.lolserver.domain.rank.application.dto.RankResponse;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankService implements RankUseCase {

    private final RankPersistencePort rankPersistencePort;

    @Override
    public Page<RankResponse> getRanks(RankSearchDto rankSearchDto, String platformId) {
        return rankPersistencePort.getRanks(rankSearchDto, platformId)
                .map(RankResponse::new);
    }
}
