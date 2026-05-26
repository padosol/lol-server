package com.example.lolserver.leaderboard.application;

import com.example.lolserver.leaderboard.application.port.in.RankUseCase;
import com.example.lolserver.leaderboard.application.port.out.RankPersistencePort;
import com.example.lolserver.leaderboard.application.model.RankReadModel;
import com.example.lolserver.leaderboard.application.dto.RankSearchDto;
import com.example.lolserver.common.support.PageResult;
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
