package com.example.lolserver.support.error;

import lombok.Getter;

@Getter
public enum ErrorType {
    DEFAULT_ERROR(500, ErrorCode.E500, "알 수 없는 오류가 발생했습니다."),

    EXTERNAL_API_ERROR(400, ErrorCode.E400, "외부 API 호출 에러"),

    NOT_FOUND_PUUID(404, ErrorCode.E404, "존재하지 않는 PUUID 입니다."),
    NOT_FOUND_MATCH_ID(404, ErrorCode.E404, "존재하지 않는 MatchId 입니다."),

    NOT_FOUND_USER(404, ErrorCode.E404, "존재하지 않는 유저 입니다."),
    NOT_FOUND_PATCH_NOTE(404, ErrorCode.E404, "존재하지 않는 패치노트 입니다."),
    NOT_FOUND_TIER_CUTOFF(404, ErrorCode.E404, "존재하지 않는 티어 컷오프입니다."),

    LOCK_ACQUISITION_FAILED(503, ErrorCode.E503, "잠시 후 다시 시도해주세요.")
    ;

    private int httpStatus;
    private ErrorCode errorCode;
    private String message;

    ErrorType(int httpStatus, ErrorCode errorCode, String message) {
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.message = message;
    }
}
