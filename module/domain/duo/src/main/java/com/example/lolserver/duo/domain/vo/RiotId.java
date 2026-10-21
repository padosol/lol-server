package com.example.lolserver.duo.domain.vo;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;

/**
 * 매칭 상대에게 보여 줄 Riot ID(gameName#tagLine). 듀오는 RSO 연동 회원만 쓰므로 연동 때 Riot 계정 API 로 받은
 * 값이고, lol-repository 에 묻지 않는다.
 *
 * <p>Riot ID 가 없으면 매칭 상대가 찾을 수 없으므로 듀오를 쓸 수 없다. 연동 때 Riot ID 를 저장하기 전(app V4 이전)에
 * 연동한 계정이 그렇다 — 다시 연동하면 채워진다.
 */
public record RiotId(String gameName, String tagLine) {

    public RiotId {
        if (isBlank(gameName) || isBlank(tagLine)) {
            throw new CoreException(ErrorType.RIOT_ACCOUNT_RELINK_REQUIRED);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
