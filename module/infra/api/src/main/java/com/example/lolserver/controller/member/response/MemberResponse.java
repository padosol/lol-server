package com.example.lolserver.controller.member.response;

import com.example.lolserver.domain.member.application.model.MemberReadModel;

public record MemberResponse(
        Long id,
        String uuid,
        String email,
        String nickname,
        String profileImageUrl
) {
    public static MemberResponse from(MemberReadModel readModel) {
        return new MemberResponse(
                readModel.getId(),
                readModel.getUuid(),
                readModel.getEmail(),
                readModel.getNickname(),
                readModel.getProfileImageUrl()
        );
    }
}
