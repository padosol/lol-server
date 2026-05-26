package com.example.lolserver.member.application.port.in;

import com.example.lolserver.member.application.dto.UpdateNicknameCommand;
import com.example.lolserver.member.application.model.MemberReadModel;

public interface MemberCommandUseCase {
    MemberReadModel updateNickname(Long memberId, UpdateNicknameCommand command);
}
