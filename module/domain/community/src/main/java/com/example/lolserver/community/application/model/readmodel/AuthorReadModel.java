package com.example.lolserver.community.application.model.readmodel;

import com.example.lolserver.member.application.model.readmodel.MemberProfileReadModel;

public record AuthorReadModel(
        Long id,
        String nickname,
        String profileImageUrl
) {
    public static AuthorReadModel of(MemberProfileReadModel profile) {
        return new AuthorReadModel(
                profile.getId(),
                profile.getNickname(),
                profile.getProfileImageUrl()
        );
    }
}
