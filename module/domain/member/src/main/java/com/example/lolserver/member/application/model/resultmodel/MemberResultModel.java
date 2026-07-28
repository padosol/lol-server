package com.example.lolserver.member.application.model.resultmodel;

import com.example.lolserver.member.application.model.readmodel.SocialAccountReadModel;
import com.example.lolserver.member.domain.Member;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MemberResultModel {

    private Long id;
    private String uuid;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String role;
    private List<SocialAccountReadModel> socialAccounts;

    public static MemberResultModel of(Member member) {
        return MemberResultModel.builder()
                .id(member.getId())
                .uuid(member.getUuid())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .role(member.getRole())
                .socialAccounts(member.getSocialAccounts().stream()
                        .map(SocialAccountReadModel::of)
                        .toList())
                .build();
    }
}
