package com.example.lolserver.domain.member.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
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

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
