package com.example.lolserver.domain.rank.application;

import com.example.lolserver.domain.rank.application.port.in.RankUseCase;
import com.example.lolserver.domain.rank.application.port.out.RankPersistencePort;
import com.example.lolserver.domain.rank.application.model.RankReadModel;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import com.example.lolserver.support.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankService implements RankUseCase {

    private final RankPersistencePort rankPersistencePort;

    @Override
    public PageResult<RankReadModel> getRanks(RankSearchDto rankSearchDto, String platformId) {
        return rankPersistencePort.getRanks(rankSearchDto, platformId)
                .map(RankReadModel::new);
    }
}
