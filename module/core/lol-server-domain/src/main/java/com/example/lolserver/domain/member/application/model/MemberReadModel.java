package com.example.lolserver.domain.member.application.model;

import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.domain.member.domain.SocialAccount;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MemberReadModel {

    private Long id;
    private String uuid;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String role;
    private List<SocialAccountReadModel> socialAccounts;

    public static MemberReadModel of(
            Member member, List<SocialAccount> socialAccounts) {
        return MemberReadModel.builder()
                .id(member.getId())
                .uuid(member.getUuid())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .role(member.getRole())
                .socialAccounts(socialAccounts.stream()
                        .map(SocialAccountReadModel::of)
                        .toList())
                .build();
    }
}
