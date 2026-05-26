package com.example.lolserver.member.application.model;

import com.example.lolserver.member.domain.Member;
import lombok.Builder;
import lombok.Getter;

/**
 * 다른 컨텍스트(community 등)에 작성자 정보를 노출하기 위한 경량 읽기 모델.
 *
 * <p>email, socialAccounts 등 민감/불필요 필드를 제외하고
 * 작성자 표시에 필요한 최소 필드(id, nickname, profileImageUrl)만 담는다.
 */
@Getter
@Builder
public class MemberProfileReadModel {

    private Long id;
    private String nickname;
    private String profileImageUrl;

    public static MemberProfileReadModel of(Member member) {
        return MemberProfileReadModel.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .build();
    }
}
