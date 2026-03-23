package com.example.lolserver.domain.community.application.port.in;

import com.example.lolserver.domain.community.application.command.VoteCommand;
import com.example.lolserver.domain.community.application.model.VoteReadModel;
import com.example.lolserver.domain.community.domain.vo.VoteTargetType;

public interface VoteUseCase {

    VoteReadModel vote(Long memberId, VoteCommand command);

    void removeVote(Long memberId, VoteTargetType targetType, Long targetId);
}
