package com.example.lolserver.domain.member.application.port.in;

import com.example.lolserver.domain.member.application.dto.UpdateNicknameCommand;
import com.example.lolserver.domain.member.application.model.MemberReadModel;

public interface MemberCommandUseCase {
    MemberReadModel updateNickname(Long memberId, UpdateNicknameCommand command);
}
