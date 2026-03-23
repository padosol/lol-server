package com.example.lolserver.domain.community.application.model;

import com.example.lolserver.domain.community.domain.vo.VoteTargetType;
import com.example.lolserver.domain.community.domain.vo.VoteType;

public record VoteReadModel(
        VoteTargetType targetType,
        Long targetId,
        VoteType voteType,
        int newUpvoteCount,
        int newDownvoteCount
) {
}
