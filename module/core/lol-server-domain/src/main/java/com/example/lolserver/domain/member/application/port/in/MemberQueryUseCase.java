package com.example.lolserver.domain.member.application.port.in;

import com.example.lolserver.domain.member.application.model.MemberReadModel;

public interface MemberQueryUseCase {
    MemberReadModel getMyProfile(Long memberId);
}
