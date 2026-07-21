package com.example.lolserver.community.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkTest {

    @DisplayName("create 는 회원/게시글을 담고 생성 시각을 찍는다")
    @Test
    void create_success() {
        // given
        LocalDateTime before = LocalDateTime.now();

        // when
        Bookmark bookmark = Bookmark.create(1L, 10L);

        // then
        assertThat(bookmark.getMemberId()).isEqualTo(1L);
        assertThat(bookmark.getPostId()).isEqualTo(10L);
        assertThat(bookmark.getCreatedAt()).isNotNull();
        assertThat(bookmark.getCreatedAt()).isAfterOrEqualTo(before);
    }

    @DisplayName("create 로 만든 북마크는 아직 저장 전이므로 id 가 없다")
    @Test
    void create_hasNoId() {
        // when
        Bookmark bookmark = Bookmark.create(1L, 10L);

        // then
        assertThat(bookmark.getId()).isNull();
    }
}
