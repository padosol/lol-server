package com.example.lolserver.repository.rank.adapter;

import com.example.lolserver.domain.rank.application.port.out.RankPersistencePort;
import com.example.lolserver.domain.rank.domain.Rank;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import com.example.lolserver.repository.rank.SummonerRankingRepository;
import com.example.lolserver.repository.rank.mapper.RankMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RankPersistenceAdapter implements RankPersistencePort {

    private final SummonerRankingRepository summonerRankingRepository;
    private final RankMapper rankMapper;

    @Override
    public List<Rank> getRanks(RankSearchDto rankSearchDto) {
        String queue = toQueueString(rankSearchDto.getRankType());
        return summonerRankingRepository.findByQueue(queue).stream()
                .map(rankMapper::entityToDomain)
                .collect(Collectors.toList());
    }

    private String toQueueString(RankSearchDto.GameType gameType) {
        return switch (gameType) {
            case SOLO -> "RANKED_SOLO_5x5";
            case FLEX -> "RANKED_FLEX_SR";
        };
    }
}
