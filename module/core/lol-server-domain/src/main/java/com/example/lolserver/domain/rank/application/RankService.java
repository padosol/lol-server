package com.example.lolserver.domain.rank.application;

import com.example.lolserver.domain.rank.application.port.in.RankUseCase;
import com.example.lolserver.domain.rank.application.port.out.RankPersistencePort;
import com.example.lolserver.domain.rank.application.dto.RankResponse;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RankService implements RankUseCase {

    private final RankPersistencePort rankPersistencePort;

    @Override
    public List<RankResponse> getRanks(RankSearchDto rankSearchDto) {
        return rankPersistencePort.getRanks(rankSearchDto).stream()
                .map(RankResponse::new)
                .toList();
    }
}
