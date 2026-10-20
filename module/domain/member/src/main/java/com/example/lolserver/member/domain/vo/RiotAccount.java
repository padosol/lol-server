package com.example.lolserver.member.domain.vo;

/**
 * RSO 로 연동한 Riot 계정 — puuid 와 연동 시점의 Riot ID(gameName#tagLine).
 *
 * <p>Riot 계정 API({@code /riot/account/v1/accounts/me})가 한 번에 준다. Riot ID 는 바뀔 수 있지만
 * 다시 연동하기 전까지는 연동 시점 값이 남는다. 다른 서비스에 묻지 않고 lol-server 가 직접 갖는
 * 회원의 계정 정보라, 듀오 매칭 상대에게 보여 줄 Riot ID 의 기본값이 된다.
 */
public record RiotAccount(String puuid, String gameName, String tagLine) {
}
