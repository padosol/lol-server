package com.example.lolserver.domain.community.domain;

import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostTest {

    @DisplayName("소유자인 경우 validateOwner 호출이 정상 통과한다")
    @Test
    void validateOwner_owner_success() {
        // given
        Post post = Post.builder()
                .id(1L)
                .memberId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        // when & then (no exception)
        post.validateOwner(1L);
    }

    @DisplayName("소유자가 아닌 경우 validateOwner 호출 시 FORBIDDEN 예외가 발생한다")
    @Test
    void validateOwner_notOwner_throwsForbidden() {
        // given
        Post post = Post.builder()
                .id(1L)
                .memberId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> post.validateOwner(2L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }

    @DisplayName("삭제되지 않은 게시글에 대해 validateNotDeleted 호출이 정상 통과한다")
    @Test
    void validateNotDeleted_active_success() {
        // given
        Post post = Post.builder()
                .id(1L)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        // when & then (no exception)
        post.validateNotDeleted();
    }

    @DisplayName("삭제된 게시글에 대해 validateNotDeleted 호출 시 POST_NOT_FOUND 예외가 발생한다")
    @Test
    void validateNotDeleted_deleted_throwsPostNotFound() {
        // given
        Post post = Post.builder()
                .id(1L)
                .deleted(true)
                .createdAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> post.validateNotDeleted())
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.POST_NOT_FOUND);
    }
}
