package com.example.lolserver.domain.member.domain;

import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String oauthProvider;
    private String oauthProviderId;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public static Member create(OAuthUserInfo userInfo) {
        return Member.builder()
                .email(userInfo.getEmail())
                .nickname(userInfo.getNickname())
                .profileImageUrl(userInfo.getProfileImageUrl())
                .oauthProvider(userInfo.getProvider())
                .oauthProviderId(userInfo.getProviderId())
                .role("USER")
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
