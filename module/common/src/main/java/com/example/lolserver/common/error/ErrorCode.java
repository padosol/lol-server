package com.example.lolserver.common.error;

public enum ErrorCode {
    E500,
    E400,
    E403,
    E404,
    E401,
    E409,
    // 업로드 남용 차단(429 Too Many Requests). 기존에는 4xx 계열에 429 가 없었다.
    E429,
    E503
}
