package com.example.lolserver.common.error;

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
    OAUTH_LOGIN_NOT_SUPPORTED(400, ErrorCode.E400,
            "해당 프로바이더는 로그인을 지원하지 않습니다."),
    SOCIAL_ACCOUNT_ALREADY_LINKED(409, ErrorCode.E409, "이미 연동된 소셜 계정입니다."),
    SOCIAL_ACCOUNT_NOT_FOUND(404, ErrorCode.E404, "존재하지 않는 소셜 계정입니다."),
    MEMBER_NOT_FOUND(404, ErrorCode.E404, "존재하지 않는 회원입니다."),

    POST_NOT_FOUND(404, ErrorCode.E404, "존재하지 않는 게시글입니다."),
    COMMENT_NOT_FOUND(404, ErrorCode.E404, "존재하지 않는 댓글입니다."),
    FORBIDDEN(403, ErrorCode.E403, "권한이 없습니다."),
    INVALID_CATEGORY(400, ErrorCode.E400, "유효하지 않은 카테고리입니다."),
    COMMENT_DEPTH_EXCEEDED(400, ErrorCode.E400, "댓글 최대 깊이를 초과했습니다."),
    VOTE_TARGET_NOT_FOUND(404, ErrorCode.E404, "투표 대상을 찾을 수 없습니다."),
    BOOKMARK_ALREADY_EXISTS(409, ErrorCode.E409, "이미 북마크한 게시글입니다."),
    BOOKMARK_NOT_FOUND(404, ErrorCode.E404, "북마크를 찾을 수 없습니다."),

    // 커뮤니티 이미지 업로드
    IMAGE_FILE_REQUIRED(400, ErrorCode.E400, "이미지 파일이 필요합니다."),
    IMAGE_SIZE_EXCEEDED(400, ErrorCode.E400, "이미지 용량이 허용 범위를 초과했습니다."),
    IMAGE_TYPE_NOT_SUPPORTED(400, ErrorCode.E400, "지원하지 않는 이미지 형식입니다."),
    IMAGE_INVALID(400, ErrorCode.E400, "이미지를 읽을 수 없습니다."),
    IMAGE_NOT_FOUND(404, ErrorCode.E404, "존재하지 않는 이미지입니다."),
    // 이미 다른 글에 붙었거나 정리된 이미지를 다시 첨부하려는 경우.
    IMAGE_NOT_ATTACHABLE(400, ErrorCode.E400, "첨부할 수 없는 상태의 이미지입니다."),
    IMAGE_COUNT_EXCEEDED(400, ErrorCode.E400, "게시글당 첨부 가능한 이미지 수를 초과했습니다."),
    IMAGE_UPLOAD_RATE_LIMITED(429, ErrorCode.E429, "업로드 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    IMAGE_STORAGE_FAILED(500, ErrorCode.E500, "이미지 저장에 실패했습니다."),

    MEMBER_ALREADY_WITHDRAWN(400, ErrorCode.E400,
            "이미 탈퇴한 회원입니다."),
    MEMBER_WITHDRAWN(403, ErrorCode.E403,
            "탈퇴한 회원입니다."),
    WITHDRAWAL_REREGISTRATION_RESTRICTED(403, ErrorCode.E403,
            "탈퇴 후 30일 이내에는 재가입할 수 없습니다."),

    // 듀오 찾기
    RIOT_ACCOUNT_NOT_LINKED(400, ErrorCode.E400, "Riot 계정 연동이 필요합니다."),
    DUO_POST_NOT_FOUND(404, ErrorCode.E404, "존재하지 않는 듀오 게시글입니다."),
    DUO_POST_NOT_ACTIVE(400, ErrorCode.E400, "활성 상태의 듀오 게시글이 아닙니다."),
    DUO_POST_ACTIVE_EXISTS(409, ErrorCode.E409,
            "이미 활성 상태의 듀오 게시글이 있습니다."),
    DUO_POST_NOT_MATCHED(400, ErrorCode.E400,
            "매칭이 완료된 게시글이 아닙니다."),
    DUO_POST_ALREADY_MATCHED(400, ErrorCode.E400,
            "이미 매칭이 완료된 게시글은 삭제할 수 없습니다."),
    DUO_POST_SELF_REQUEST(400, ErrorCode.E400,
            "본인의 듀오 게시글에는 요청할 수 없습니다."),
    DUO_REQUEST_NOT_FOUND(404, ErrorCode.E404, "존재하지 않는 듀오 요청입니다."),
    DUO_REQUEST_ALREADY_EXISTS(409, ErrorCode.E409,
            "이미 해당 게시글에 요청을 보냈습니다."),
    DUO_REQUEST_NOT_PENDING(400, ErrorCode.E400,
            "대기 상태의 요청만 수락할 수 있습니다."),
    DUO_REQUEST_NOT_ACCEPTED(400, ErrorCode.E400,
            "수락된 요청만 확인할 수 있습니다."),
    DUO_REQUEST_ALREADY_COMPLETED(400, ErrorCode.E400,
            "이미 처리 완료된 요청입니다."),
    INVALID_LANE(400, ErrorCode.E400, "유효하지 않은 라인입니다."),
    // 전적 데이터가 적재된 적 없는 계정과 실제 언랭 계정은 사용자가 취할 행동이 다르므로 구분한다.
    SUMMONER_SEARCH_REQUIRED(400, ErrorCode.E400,
            "전적 정보가 없습니다. 소환사 검색을 먼저 진행해주세요."),
    DUO_UNRANKED_NOT_ALLOWED(400, ErrorCode.E400,
            "솔로랭크 티어가 없는 계정은 듀오를 등록할 수 없습니다.");

    private final int httpStatus;
    private final ErrorCode errorCode;
    private final String message;

    ErrorType(int httpStatus, ErrorCode errorCode, String message) {
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.message = message;
    }
}
