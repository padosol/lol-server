package com.example.lolserver.repository.rank.adapter;

import com.example.lolserver.QueueType;
import com.example.lolserver.domain.rank.application.port.out.RankPersistencePort;
import com.example.lolserver.domain.rank.domain.Rank;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import com.example.lolserver.repository.rank.RankRepository;
import com.example.lolserver.repository.rank.mapper.RankMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RankPersistenceAdapter implements RankPersistencePort {

    private final RankRepository rankRepository;
    private final RankMapper rankMapper;

    @Override
    public List<Rank> getRanks(RankSearchDto rankSearchDto) {
        QueueType queueType = toQueueType(rankSearchDto.getType());
        return rankRepository.findByQueueType(queueType).stream()
                .map(rankMapper::entityToDomain)
                .collect(Collectors.toList());
    }

    private QueueType toQueueType(RankSearchDto.GameType gameType) {
        return switch (gameType) {
            case SOLO -> QueueType.RANKED_SOLO_5x5;
            case FLEX -> QueueType.RANKED_FLEX_SR;
        };
    }
}
