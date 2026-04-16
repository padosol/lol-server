package com.example.lolserver.domain.community.application.model;

import com.example.lolserver.domain.member.domain.Member;

public record AuthorReadModel(
        Long id,
        String nickname,
        String profileImageUrl
) {
    public static AuthorReadModel of(Member member) {
        return new AuthorReadModel(
                member.getId(),
                member.getNickname(),
                member.getProfileImageUrl()
        );
    }
}
