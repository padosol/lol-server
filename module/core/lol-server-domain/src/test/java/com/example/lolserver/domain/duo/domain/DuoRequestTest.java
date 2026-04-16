package com.example.lolserver.domain.duo.domain;

import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DuoRequestTest {

    @DisplayName("요청자인 경우 validateRequester 호출이 정상 통과한다")
    @Test
    void validateRequester_requester_success() {
        // given
        DuoRequest duoRequest = DuoRequest.builder()
                .id(1L)
                .requesterId(1L)
                .build();

        // when & then (no exception)
        duoRequest.validateRequester(1L);
    }

    @DisplayName("요청자가 아닌 경우 validateRequester 호출 시 FORBIDDEN 예외가 발생한다")
    @Test
    void validateRequester_notRequester_throwsForbidden() {
        // given
        DuoRequest duoRequest = DuoRequest.builder()
                .id(1L)
                .requesterId(1L)
                .build();

        // when & then
        assertThatThrownBy(() -> duoRequest.validateRequester(2L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }
}
