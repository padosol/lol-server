package com.example.lolserver.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientExceptionTest {

    @DisplayName("RestClientException을 생성할 수 있다")
    @Test
    void constructor_validParams_createsException() {
        // given
        ErrorType errorType = ErrorType.NOT_FOUND_USER;
        String message = "테스트 에러 메시지";

        // when
        RestClientException exception = new RestClientException(errorType, message);

        // then
        assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND_USER);
        assertThat(exception.getMessage()).isEqualTo("테스트 에러 메시지");
    }

    @DisplayName("EXTERNAL_API_ERROR 타입으로 예외를 생성할 수 있다")
    @Test
    void constructor_externalApiError_createsException() {
        // given
        ErrorType errorType = ErrorType.EXTERNAL_API_ERROR;
        String message = "외부 API 호출 실패";

        // when
        RestClientException exception = new RestClientException(errorType, message);

        // then
        assertThat(exception.getErrorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
        assertThat(exception.getErrorType().getHttpStatus()).isEqualTo(400);
        assertThat(exception.getErrorType().getErrorCode()).isEqualTo(ErrorCode.E400);
    }
}
