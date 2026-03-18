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

    INVALID_TIER_FILTER(400, ErrorCode.E400, "유효하지 않은 티어 필터입니다."),

    LOCK_ACQUISITION_FAILED(503, ErrorCode.E503, "잠시 후 다시 시도해주세요."),

    INVALID_INPUT(400, ErrorCode.E400, "잘못된 입력값입니다."),

    UNAUTHORIZED(401, ErrorCode.E401, "인증이 필요합니다."),
    INVALID_TOKEN(401, ErrorCode.E401, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(401, ErrorCode.E401, "만료된 토큰입니다."),
    OAUTH_LOGIN_FAILED(400, ErrorCode.E400, "OAuth 로그인에 실패했습니다."),
    RIOT_ACCOUNT_ALREADY_LINKED(409, ErrorCode.E409, "이미 연동된 Riot 계정입니다."),
    RIOT_LINK_NOT_FOUND(404, ErrorCode.E404, "존재하지 않는 Riot 계정 연동입니다."),
    MEMBER_NOT_FOUND(404, ErrorCode.E404, "존재하지 않는 회원입니다."),
    INVALID_OAUTH_STATE(400, ErrorCode.E400, "유효하지 않은 OAuth state 입니다.");

    private final int httpStatus;
    private final ErrorCode errorCode;
    private final String message;

    ErrorType(int httpStatus, ErrorCode errorCode, String message) {
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.message = message;
    }
}
