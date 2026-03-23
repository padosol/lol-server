package com.example.lolserver.controller.community.response;

import com.example.lolserver.domain.community.application.model.VoteReadModel;

public record VoteResponse(
        String targetType,
        Long targetId,
        String voteType,
        int newUpvoteCount,
        int newDownvoteCount
) {
    public static VoteResponse from(VoteReadModel readModel) {
        return new VoteResponse(
                readModel.targetType().name(),
                readModel.targetId(),
                readModel.voteType().name(),
                readModel.newUpvoteCount(),
                readModel.newDownvoteCount()
        );
    }
}
