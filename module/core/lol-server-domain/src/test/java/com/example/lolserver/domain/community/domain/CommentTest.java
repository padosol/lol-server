package com.example.lolserver.domain.community.domain;

import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentTest {

    @DisplayName("소유자인 경우 validateOwner 호출이 정상 통과한다")
    @Test
    void validateOwner_owner_success() {
        // given
        Comment comment = Comment.builder()
                .id(1L)
                .memberId(1L)
                .build();

        // when & then (no exception)
        comment.validateOwner(1L);
    }

    @DisplayName("소유자가 아닌 경우 validateOwner 호출 시 FORBIDDEN 예외가 발생한다")
    @Test
    void validateOwner_notOwner_throwsForbidden() {
        // given
        Comment comment = Comment.builder()
                .id(1L)
                .memberId(1L)
                .build();

        // when & then
        assertThatThrownBy(() -> comment.validateOwner(2L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }
}
