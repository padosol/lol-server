package com.example.lolserver.domain.member.application.model;

import com.example.lolserver.domain.member.domain.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberReadModel {

    private Long id;
    private String uuid;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String role;

    public static MemberReadModel of(Member member) {
        return MemberReadModel.builder()
                .id(member.getId())
                .uuid(member.getUuid())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .role(member.getRole())
                .build();
    }
}
