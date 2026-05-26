package com.example.lolserver.community.adapter.in.web.response;

import com.example.lolserver.community.application.model.VoteReadModel;

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
