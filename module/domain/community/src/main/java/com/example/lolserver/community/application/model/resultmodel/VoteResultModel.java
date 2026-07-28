package com.example.lolserver.community.application.model.resultmodel;

import com.example.lolserver.community.domain.vo.VoteTargetType;
import com.example.lolserver.community.domain.vo.VoteType;

public record VoteResultModel(
        VoteTargetType targetType,
        Long targetId,
        VoteType voteType,
        int newUpvoteCount,
        int newDownvoteCount
) {
}
