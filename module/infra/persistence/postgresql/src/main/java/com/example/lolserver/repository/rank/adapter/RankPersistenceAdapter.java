package com.example.lolserver.repository.rank.adapter;

import com.example.lolserver.domain.rank.application.port.out.RankPersistencePort;
import com.example.lolserver.domain.rank.domain.Rank;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import com.example.lolserver.repository.rank.SummonerRankingRepository;
import com.example.lolserver.repository.rank.entity.SummonerRankingEntity;
import com.example.lolserver.repository.rank.mapper.RankMapper;
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
    public Page<Rank> getRanks(RankSearchDto rankSearchDto, String region) {
        String queue = toQueueString(rankSearchDto.getRankType());
        Pageable pageable = PageRequest.of(
                rankSearchDto.getPage() - 1,
                PAGE_SIZE,
                Sort.by("currentRank").ascending()
        );

        Page<SummonerRankingEntity> entityPage;
        if (rankSearchDto.getTier() != null && !rankSearchDto.getTier().isEmpty()) {
            entityPage = summonerRankingRepository.findByQueueAndTier(queue, rankSearchDto.getTier(), pageable);
        } else {
            entityPage = summonerRankingRepository.findByQueue(queue, pageable);
        }

        return entityPage.map(rankMapper::entityToDomain);
    }

    private String toQueueString(RankSearchDto.GameType gameType) {
        return switch (gameType) {
            case SOLO -> "RANKED_SOLO_5x5";
            case FLEX -> "RANKED_FLEX_SR";
        };
    }
}
