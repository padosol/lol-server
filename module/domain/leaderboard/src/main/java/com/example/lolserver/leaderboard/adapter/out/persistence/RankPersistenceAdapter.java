package com.example.lolserver.leaderboard.adapter.out.persistence;

import com.example.lolserver.shared.QueueType;
import com.example.lolserver.leaderboard.application.port.out.RankPersistencePort;
import com.example.lolserver.leaderboard.domain.Rank;
import com.example.lolserver.leaderboard.application.dto.RankSearchDto;
import com.example.lolserver.leaderboard.adapter.out.persistence.entity.SummonerRankingEntity;
import com.example.lolserver.leaderboard.adapter.out.persistence.mapper.RankMapper;
import com.example.lolserver.common.support.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankPersistenceAdapter implements RankPersistencePort {

    private static final int PAGE_SIZE = 50;

    private final SummonerRankingRepository summonerRankingRepository;
    private final RankMapper rankMapper;

    @Override
    public PageResult<Rank> getRanks(RankSearchDto rankSearchDto, String platformId) {
        String queue = toQueueString(rankSearchDto.getRankType());
        Pageable pageable = PageRequest.of(
                rankSearchDto.getPage() - 1,
                PAGE_SIZE,
                Sort.by("currentRank").ascending()
        );

        String normalizedPlatformId = platformId.toUpperCase();

        Page<SummonerRankingEntity> entityPage;
        if (rankSearchDto.getTier() != null && !rankSearchDto.getTier().isEmpty()) {
            entityPage = summonerRankingRepository.findByPlatformIdAndQueueAndTier(
                    normalizedPlatformId, queue, rankSearchDto.getTier(), pageable);
        } else {
            entityPage = summonerRankingRepository.findByPlatformIdAndQueue(normalizedPlatformId, queue, pageable);
        }

        Page<Rank> domainPage = entityPage.map(rankMapper::entityToDomain);
        return toPageResult(domainPage);
    }

    private <T> PageResult<T> toPageResult(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    private String toQueueString(RankSearchDto.GameType gameType) {
        return switch (gameType) {
            case SOLO -> QueueType.RANKED_SOLO_5x5.name();
            case FLEX -> QueueType.RANKED_FLEX_SR.name();
        };
    }
}
