package com.example.lolserver.community.application.port.in;

import com.example.lolserver.community.application.command.VoteCommand;
import com.example.lolserver.community.application.model.resultmodel.VoteResultModel;
import com.example.lolserver.community.domain.vo.VoteTargetType;

public interface VoteUseCase {

    VoteResultModel vote(Long memberId, VoteCommand command);

    void removeVote(Long memberId, VoteTargetType targetType, Long targetId);

    /**
     * 멱등 취소 ({@code DELETE .../vote}). 투표가 없으면 아무것도 하지 않는다.
     */
    void removeVoteIfPresent(Long memberId, VoteTargetType targetType, Long targetId);
}
