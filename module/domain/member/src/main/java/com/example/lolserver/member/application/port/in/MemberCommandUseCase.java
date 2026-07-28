package com.example.lolserver.member.application.port.in;

import com.example.lolserver.member.application.dto.UpdateNicknameCommand;
import com.example.lolserver.member.application.model.resultmodel.MemberResultModel;

public interface MemberCommandUseCase {
    MemberResultModel updateNickname(Long memberId, UpdateNicknameCommand command);
}
