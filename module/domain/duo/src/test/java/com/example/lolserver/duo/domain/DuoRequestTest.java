package com.example.lolserver.duo.domain;

import com.example.lolserver.duo.domain.vo.DuoRequestStatus;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

    @DisplayName("PENDING 요청에 close 호출 시 CLOSED로 전환된다")
    @Test
    void close_pending_closes() {
        // given
        DuoRequest duoRequest = createRequestWithStatus(DuoRequestStatus.PENDING);

        // when
        duoRequest.close();

        // then
        assertThat(duoRequest.getStatus()).isEqualTo(DuoRequestStatus.CLOSED);
    }

    @DisplayName("ACCEPTED 요청에 close 호출 시 CLOSED로 전환된다")
    @Test
    void close_accepted_closes() {
        // given
        DuoRequest duoRequest = createRequestWithStatus(DuoRequestStatus.ACCEPTED);

        // when
        duoRequest.close();

        // then
        assertThat(duoRequest.getStatus()).isEqualTo(DuoRequestStatus.CLOSED);
    }

    @DisplayName("CONFIRMED 요청에 close 호출 시 DUO_REQUEST_ALREADY_COMPLETED 예외가 발생한다")
    @Test
    void close_confirmed_throwsAlreadyCompleted() {
        // given
        DuoRequest duoRequest = createRequestWithStatus(DuoRequestStatus.CONFIRMED);

        // when & then
        assertThatThrownBy(duoRequest::close)
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.DUO_REQUEST_ALREADY_COMPLETED);
    }

    @DisplayName("CANCELLED 요청에 close 호출 시 DUO_REQUEST_ALREADY_COMPLETED 예외가 발생한다")
    @Test
    void close_cancelled_throwsAlreadyCompleted() {
        // given
        DuoRequest duoRequest = createRequestWithStatus(DuoRequestStatus.CANCELLED);

        // when & then
        assertThatThrownBy(duoRequest::close)
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.DUO_REQUEST_ALREADY_COMPLETED);
    }

    private DuoRequest createRequestWithStatus(DuoRequestStatus status) {
        return DuoRequest.builder()
                .id(1L)
                .duoPostId(100L)
                .requesterId(2L)
                .status(status)
                .build();
    }
}
