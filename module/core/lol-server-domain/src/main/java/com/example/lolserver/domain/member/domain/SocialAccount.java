package com.example.lolserver.domain.member.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class SocialAccount {

    private Long id;
    private Long memberId;
    private String provider;
    private String providerId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String puuid;
    private LocalDateTime linkedAt;

    public static SocialAccount create(Long memberId, String provider,
            String providerId, String email, String nickname,
            String profileImageUrl, String puuid) {
        return SocialAccount.builder()
                .memberId(memberId)
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .puuid(puuid)
                .linkedAt(LocalDateTime.now())
                .build();
    }

    public void anonymize() {
        this.email = null;
        this.nickname = null;
        this.profileImageUrl = null;
        this.puuid = null;
        this.providerId = "withdrawn_" + this.id + "_"
                + this.provider;
    }
}
