package com.example.lolserver.community.application.port.in;

import com.example.lolserver.community.application.command.VoteCommand;
import com.example.lolserver.community.application.model.VoteReadModel;
import com.example.lolserver.community.domain.vo.VoteTargetType;

public interface VoteUseCase {

    VoteReadModel vote(Long memberId, VoteCommand command);

    void removeVote(Long memberId, VoteTargetType targetType, Long targetId);
}
