package com.example.lolserver.member.domain;

import com.example.lolserver.member.domain.vo.RiotAccount;
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
    /** RIOT 연동 시점의 Riot ID. V4 이전에 연동한 계정은 NULL — 다시 연동하면 채워진다. */
    private String gameName;
    private String tagLine;
    private LocalDateTime linkedAt;

    /**
     * @param riotAccount RIOT 연동이면 puuid·Riot ID, 다른 provider 면 null
     */
    public static SocialAccount create(Long memberId, String provider,
            String providerId, String email, String nickname,
            String profileImageUrl, RiotAccount riotAccount) {
        return SocialAccount.builder()
                .memberId(memberId)
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .puuid(riotAccount == null ? null : riotAccount.puuid())
                .gameName(riotAccount == null ? null : riotAccount.gameName())
                .tagLine(riotAccount == null ? null : riotAccount.tagLine())
                .linkedAt(LocalDateTime.now())
                .build();
    }

    public void anonymize() {
        this.email = null;
        this.nickname = null;
        this.profileImageUrl = null;
        this.puuid = null;
        this.gameName = null;
        this.tagLine = null;
        this.providerId = "withdrawn_" + this.id + "_"
                + this.provider;
    }
}
