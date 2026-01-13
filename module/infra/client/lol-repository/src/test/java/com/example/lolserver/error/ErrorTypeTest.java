package com.example.lolserver.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.LogLevel;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorTypeTest {

    @DisplayName("DEFAULT_ERROR는 500 상태 코드와 ERROR 로그 레벨을 가진다")
    @Test
    void defaultError_hasCorrectProperties() {
        // given
        ErrorType errorType = ErrorType.DEFAULT_ERROR;

        // then
        assertThat(errorType.getHttpStatus()).isEqualTo(500);
        assertThat(errorType.getErrorCode()).isEqualTo(ErrorCode.E500);
        assertThat(errorType.getMessage()).isEqualTo("알 수 없는 오류가 발생했습니다.");
        assertThat(errorType.getLogLevel()).isEqualTo(LogLevel.ERROR);
    }

    @DisplayName("EXTERNAL_API_ERROR는 400 상태 코드와 INFO 로그 레벨을 가진다")
    @Test
    void externalApiError_hasCorrectProperties() {
        // given
        ErrorType errorType = ErrorType.EXTERNAL_API_ERROR;

        // then
        assertThat(errorType.getHttpStatus()).isEqualTo(400);
        assertThat(errorType.getErrorCode()).isEqualTo(ErrorCode.E400);
        assertThat(errorType.getMessage()).isEqualTo("외부 API 호출 에러");
        assertThat(errorType.getLogLevel()).isEqualTo(LogLevel.INFO);
    }

    @DisplayName("NOT_FOUND_PUUID는 404 상태 코드를 가진다")
    @Test
    void notFoundPuuid_hasCorrectProperties() {
        // given
        ErrorType errorType = ErrorType.NOT_FOUND_PUUID;

        // then
        assertThat(errorType.getHttpStatus()).isEqualTo(404);
        assertThat(errorType.getErrorCode()).isEqualTo(ErrorCode.E404);
        assertThat(errorType.getMessage()).isEqualTo("존재하지 않는 PUUID 입니다.");
    }

    @DisplayName("NOT_FOUND_MATCH_ID는 404 상태 코드를 가진다")
    @Test
    void notFoundMatchId_hasCorrectProperties() {
        // given
        ErrorType errorType = ErrorType.NOT_FOUND_MATCH_ID;

        // then
        assertThat(errorType.getHttpStatus()).isEqualTo(404);
        assertThat(errorType.getErrorCode()).isEqualTo(ErrorCode.E404);
        assertThat(errorType.getMessage()).isEqualTo("존재하지 않는 MatchId 입니다.");
    }

    @DisplayName("NOT_FOUND_USER는 404 상태 코드를 가진다")
    @Test
    void notFoundUser_hasCorrectProperties() {
        // given
        ErrorType errorType = ErrorType.NOT_FOUND_USER;

        // then
        assertThat(errorType.getHttpStatus()).isEqualTo(404);
        assertThat(errorType.getErrorCode()).isEqualTo(ErrorCode.E404);
        assertThat(errorType.getMessage()).isEqualTo("존재하지 않는 유저 입니다.");
    }
}
