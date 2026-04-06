package com.example.lolserver.domain.member.domain;

import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Getter
@Builder
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
}
