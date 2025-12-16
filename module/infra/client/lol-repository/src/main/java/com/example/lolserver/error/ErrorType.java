package com.example.lolserver.error;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;

@Getter
public enum ErrorType {
    DEFAULT_ERROR(500, ErrorCode.E500, "알 수 없는 오류가 발생했습니다.", LogLevel.ERROR),

    EXTERNAL_API_ERROR(400, ErrorCode.E400, "외부 API 호출 에러", LogLevel.INFO),

    NOT_FOUND_PUUID(404, ErrorCode.E404, "존재하지 않는 PUUID 입니다.", LogLevel.INFO),
    NOT_FOUND_MATCH_ID(404, ErrorCode.E404, "존재하지 않는 MatchId 입니다.", LogLevel.INFO),

    NOT_FOUND_USER(404, ErrorCode.E404, "존재하지 않는 유저 입니다.", LogLevel.INFO)
    ;

    private int httpStatus;
    private ErrorCode errorCode;
    private String message;
    private LogLevel logLevel;

    ErrorType(int httpStatus, ErrorCode errorCode, String message, LogLevel logLevel) {
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.message = message;
        this.logLevel = logLevel;
    }
}
