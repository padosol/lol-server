package com.example.lolserver.community.application.model;

import com.example.lolserver.community.domain.vo.VoteTargetType;
import com.example.lolserver.community.domain.vo.VoteType;

public record VoteReadModel(
        VoteTargetType targetType,
        Long targetId,
        VoteType voteType,
        int newUpvoteCount,
        int newDownvoteCount
) {
}
