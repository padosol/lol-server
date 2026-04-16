package com.example.lolserver.domain.duo.domain;

import com.example.lolserver.domain.duo.domain.vo.DuoPostStatus;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DuoPostTest {

    @DisplayName("소유자인 경우 validateOwner 호출이 정상 통과한다")
    @Test
    void validateOwner_owner_success() {
        // given
        DuoPost duoPost = createActiveDuoPost(1L);

        // when & then (no exception)
        duoPost.validateOwner(1L);
    }

    @DisplayName("소유자가 아닌 경우 validateOwner 호출 시 FORBIDDEN 예외가 발생한다")
    @Test
    void validateOwner_notOwner_throwsForbidden() {
        // given
        DuoPost duoPost = createActiveDuoPost(1L);

        // when & then
        assertThatThrownBy(() -> duoPost.validateOwner(2L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }

    @DisplayName("소유자가 아닌 경우 validateNotOwner 호출이 정상 통과한다")
    @Test
    void validateNotOwner_notOwner_success() {
        // given
        DuoPost duoPost = createActiveDuoPost(1L);

        // when & then (no exception)
        duoPost.validateNotOwner(2L);
    }

    @DisplayName("소유자인 경우 validateNotOwner 호출 시 DUO_POST_SELF_REQUEST 예외가 발생한다")
    @Test
    void validateNotOwner_owner_throwsSelfRequest() {
        // given
        DuoPost duoPost = createActiveDuoPost(1L);

        // when & then
        assertThatThrownBy(() -> duoPost.validateNotOwner(1L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.DUO_POST_SELF_REQUEST);
    }

    @DisplayName("활성 상태의 게시글에 대해 validateActive 호출이 정상 통과한다")
    @Test
    void validateActive_active_success() {
        // given
        DuoPost duoPost = createActiveDuoPost(1L);

        // when & then (no exception)
        duoPost.validateActive();
    }

    @DisplayName("만료된 게시글에 대해 validateActive 호출 시 DUO_POST_NOT_ACTIVE 예외가 발생한다")
    @Test
    void validateActive_expired_throwsNotActive() {
        // given
        DuoPost duoPost = DuoPost.builder()
                .id(1L)
                .memberId(1L)
                .status(DuoPostStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        // when & then
        assertThatThrownBy(() -> duoPost.validateActive())
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.DUO_POST_NOT_ACTIVE);
    }

    @DisplayName("MATCHED 상태의 게시글에 대해 validateActive 호출 시 DUO_POST_NOT_ACTIVE 예외가 발생한다")
    @Test
    void validateActive_matched_throwsNotActive() {
        // given
        DuoPost duoPost = DuoPost.builder()
                .id(1L)
                .memberId(1L)
                .status(DuoPostStatus.MATCHED)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        // when & then
        assertThatThrownBy(() -> duoPost.validateActive())
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.DUO_POST_NOT_ACTIVE);
    }

    private DuoPost createActiveDuoPost(Long memberId) {
        return DuoPost.builder()
                .id(1L)
                .memberId(memberId)
                .status(DuoPostStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }
}
