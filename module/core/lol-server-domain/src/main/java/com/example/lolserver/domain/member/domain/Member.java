package com.example.lolserver.domain.member.domain;

import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    private static final String[] ADJECTIVES = {
        "용감한", "빛나는", "신비한", "강력한", "민첩한",
        "현명한", "고요한", "열정적인", "화려한", "당당한"
    };
    private static final String[] NOUNS = {
        "소환사", "전사", "마법사", "수호자", "탐험가",
        "챔피언", "사냥꾼", "기사", "영웅", "도전자"
    };
    private static final Random RANDOM = new Random();

    private Long id;
    private String uuid;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime withdrawnAt;
    private List<SocialAccount> socialAccounts;

    public static Member createNew() {
        return Member.builder()
                .uuid(UUID.randomUUID().toString())
                .nickname(generateRandomNickname())
                .role("USER")
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    private static String generateRandomNickname() {
        String adjective = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
        int number = RANDOM.nextInt(10000);
        return adjective + noun + number;
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

    public List<SocialAccount> getSocialAccounts() {
        return socialAccounts == null
                ? List.of()
                : Collections.unmodifiableList(socialAccounts);
    }

    public boolean isSocialAccountsLoaded() {
        return socialAccounts != null;
    }

    public void linkSocialAccount(String provider,
            String providerId, String email, String nickname,
            String profileImageUrl) {
        if (this.socialAccounts == null) {
            this.socialAccounts = new ArrayList<>();
        }
        boolean alreadyHasProvider = this.socialAccounts.stream()
                .anyMatch(sa -> sa.getProvider().equals(provider));
        if (alreadyHasProvider) {
            throw new CoreException(
                    ErrorType.SOCIAL_ACCOUNT_ALREADY_LINKED);
        }
        SocialAccount newAccount = SocialAccount.create(
                this.id, provider, providerId, email, nickname,
                profileImageUrl);
        this.socialAccounts.add(newAccount);
    }

    public void withdraw() {
        if (this.withdrawnAt != null) {
            throw new CoreException(
                    ErrorType.MEMBER_ALREADY_WITHDRAWN);
        }
        this.withdrawnAt = LocalDateTime.now();
        this.email = "withdrawn_" + this.uuid;
        this.nickname = "탈퇴한회원_"
                + UUID.randomUUID().toString().substring(0, 8);
        this.profileImageUrl = null;
        if (this.socialAccounts != null) {
            this.socialAccounts.forEach(SocialAccount::anonymize);
        }
    }

    public boolean isWithdrawn() {
        return this.withdrawnAt != null;
    }

    public void unlinkSocialAccount(Long socialAccountId) {
        if (this.socialAccounts == null
                || this.socialAccounts.isEmpty()) {
            throw new CoreException(
                    ErrorType.SOCIAL_ACCOUNT_NOT_FOUND);
        }
        SocialAccount target = this.socialAccounts.stream()
                .filter(sa -> sa.getId().equals(socialAccountId))
                .findFirst()
                .orElseThrow(() -> new CoreException(
                        ErrorType.SOCIAL_ACCOUNT_NOT_FOUND));
        this.socialAccounts.remove(target);
    }

    public static Member createNewWithSocialAccount(String provider,
            String providerId, String email, String nickname,
            String profileImageUrl) {
        Member member = createNew();
        member.socialAccounts = new ArrayList<>();
        SocialAccount account = SocialAccount.create(
                null, provider, providerId, email, nickname,
                profileImageUrl);
        member.socialAccounts.add(account);
        return member;
    }
}
