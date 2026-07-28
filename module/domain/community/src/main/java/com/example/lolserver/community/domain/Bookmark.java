package com.example.lolserver.community.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Bookmark {

    private Long id;
    private Long memberId;
    private Long postId;
    private LocalDateTime createdAt;

    public static Bookmark create(Long memberId, Long postId) {
        return Bookmark.builder()
                .memberId(memberId)
                .postId(postId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
