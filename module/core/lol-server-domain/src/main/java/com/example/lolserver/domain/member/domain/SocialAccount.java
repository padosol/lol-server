package com.example.lolserver.domain.member.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialAccount {

    private Long id;
    private Long memberId;
    private String provider;
    private String providerId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private LocalDateTime linkedAt;

    public static SocialAccount create(Long memberId, String provider,
            String providerId, String email, String nickname,
            String profileImageUrl) {
        return SocialAccount.builder()
                .memberId(memberId)
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .linkedAt(LocalDateTime.now())
                .build();
    }
}
