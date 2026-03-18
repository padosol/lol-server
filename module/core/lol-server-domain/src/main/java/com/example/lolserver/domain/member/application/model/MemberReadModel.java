package com.example.lolserver.domain.member.application.model;

import com.example.lolserver.domain.member.domain.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberReadModel {

    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String oauthProvider;
    private String role;

    public static MemberReadModel of(Member member) {
        return MemberReadModel.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .oauthProvider(member.getOauthProvider())
                .role(member.getRole())
                .build();
    }
}
