package com.example.lolserver.community.application.port.out;

import com.example.lolserver.community.domain.Vote;
import com.example.lolserver.community.domain.vo.VoteTargetType;
import com.example.lolserver.community.domain.vo.VoteType;

import java.util.Optional;

public interface VotePersistencePort {

    Vote save(Vote vote);

    Optional<Vote> findByMemberIdAndTargetTypeAndTargetId(Long memberId, VoteTargetType targetType, Long targetId);

    void delete(Vote vote);

    int countByTargetTypeAndTargetIdAndVoteType(
            VoteTargetType targetType, Long targetId, VoteType voteType);
}
