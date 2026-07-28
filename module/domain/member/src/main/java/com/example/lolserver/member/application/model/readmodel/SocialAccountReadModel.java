package com.example.lolserver.member.application.model.readmodel;

import com.example.lolserver.member.domain.SocialAccount;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SocialAccountReadModel {

    private Long id;
    private String provider;
    private String providerId;
    private String email;
    private String nickname;
    private String puuid;
    private LocalDateTime linkedAt;

    public static SocialAccountReadModel of(SocialAccount account) {
        return SocialAccountReadModel.builder()
                .id(account.getId())
                .provider(account.getProvider())
                .providerId(account.getProviderId())
                .email(account.getEmail())
                .nickname(account.getNickname())
                .puuid(account.getPuuid())
                .linkedAt(account.getLinkedAt())
                .build();
    }
}
