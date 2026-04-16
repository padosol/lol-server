package com.example.lolserver.repository.community.adapter;

import com.example.lolserver.domain.community.application.port.out.VotePersistencePort;
import com.example.lolserver.domain.community.domain.Vote;
import com.example.lolserver.domain.community.domain.vo.VoteTargetType;
import com.example.lolserver.domain.community.domain.vo.VoteType;
import com.example.lolserver.repository.community.entity.CommunityVoteEntity;
import com.example.lolserver.repository.community.mapper.CommunityVoteMapper;
import com.example.lolserver.repository.community.repository.CommunityVoteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VotePersistenceAdapter implements VotePersistencePort {

    private final CommunityVoteJpaRepository voteJpaRepository;
    private final CommunityVoteMapper voteMapper;

    @Override
    public Vote save(Vote vote) {
        CommunityVoteEntity entity = voteMapper.toEntity(vote);
        CommunityVoteEntity saved = voteJpaRepository.save(entity);
        return voteMapper.toDomain(saved);
    }

    @Override
    public Optional<Vote> findByMemberIdAndTargetTypeAndTargetId(
            Long memberId, VoteTargetType targetType, Long targetId) {
        return voteJpaRepository
                .findByMemberIdAndTargetTypeAndTargetId(memberId, targetType.name(), targetId)
                .map(voteMapper::toDomain);
    }

    @Override
    public void delete(Vote vote) {
        voteJpaRepository.deleteById(vote.getId());
    }

    @Override
    public int countByTargetTypeAndTargetIdAndVoteType(VoteTargetType targetType, Long targetId, VoteType voteType) {
        return voteJpaRepository.countByTargetTypeAndTargetIdAndVoteType(
                targetType.name(), targetId, voteType.name());
    }
}
