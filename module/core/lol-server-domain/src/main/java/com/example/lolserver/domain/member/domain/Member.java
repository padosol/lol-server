package com.example.lolserver.domain.member.domain;

import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
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

    public static Member createFromOAuth(String email, String nickname,
            String profileImageUrl, String oauthProvider,
            String oauthProviderId) {
        return Member.builder()
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .oauthProvider(oauthProvider)
                .oauthProviderId(oauthProviderId)
                .role("USER")
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new CoreException(ErrorType.INVALID_INPUT,
                    "닉네임은 필수 입력값입니다.");
        }
        String trimmed = nickname.trim();
        if (trimmed.length() < 2 || trimmed.length() > 20) {
            throw new CoreException(ErrorType.INVALID_INPUT,
                    "닉네임은 2자 이상 20자 이하로 입력해주세요.");
        }
        this.nickname = trimmed;
    }
}
