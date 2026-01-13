package com.example.lolserver.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMessageTest {

    @DisplayName("ErrorType으로 ErrorMessage를 생성할 수 있다")
    @Test
    void constructor_validErrorType_createsMessage() {
        // given
        ErrorType errorType = ErrorType.NOT_FOUND_USER;

        // when
        ErrorMessage errorMessage = new ErrorMessage(errorType);

        // then
        assertThat(errorMessage.getErrorCode()).isEqualTo("E404");
        assertThat(errorMessage.getMessage()).isEqualTo("존재하지 않는 유저 입니다.");
        assertThat(errorMessage.getTimestamp()).isNotNull();
    }

    @DisplayName("DEFAULT_ERROR로 ErrorMessage를 생성할 수 있다")
    @Test
    void constructor_defaultError_createsMessage() {
        // given
        ErrorType errorType = ErrorType.DEFAULT_ERROR;

        // when
        ErrorMessage errorMessage = new ErrorMessage(errorType);

        // then
        assertThat(errorMessage.getErrorCode()).isEqualTo("E500");
        assertThat(errorMessage.getMessage()).isEqualTo("알 수 없는 오류가 발생했습니다.");
    }

    @DisplayName("기본 생성자로 ErrorMessage를 생성할 수 있다")
    @Test
    void noArgsConstructor_createsEmptyMessage() {
        // when
        ErrorMessage errorMessage = new ErrorMessage();

        // then
        assertThat(errorMessage.getErrorCode()).isNull();
        assertThat(errorMessage.getMessage()).isNull();
        assertThat(errorMessage.getTimestamp()).isNull();
    }
}
