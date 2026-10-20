package com.example.lolserver.duo.application;

import com.example.lolserver.duo.application.port.out.RiotAccountStatsPort;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiotAccountResolver {

    private final MemberQueryUseCase memberQueryUseCase;
    private final RiotAccountStatsPort riotAccountStatsPort;

    public String extractRiotPuuid(Long memberId) {
        return memberQueryUseCase.findRiotPuuid(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.RIOT_ACCOUNT_NOT_LINKED));
    }

    /**
     * 전적 데이터가 적재된 적 없는 계정을 언랭과 구분해 걸러낸다.
     *
     * <p>색인된 적 없는 소환사는 리그·매치 기록이 없어 실제 티어와 무관하게 언랭처럼 보인다.
     * lol-repository 는 이 경우 404(빈 값)로 구분해 준다. 언랭으로 처리하면 "언랭이라 등록 불가"라는
     * 잘못된 안내가 나가므로, 소환사 검색을 먼저 하도록 별도 에러로 알린다.
     */
    public RiotAccountStats lookupAllStats(String puuid) {
        return riotAccountStatsPort.findByPuuid(puuid)
                .orElseThrow(() -> new CoreException(ErrorType.SUMMONER_SEARCH_REQUIRED));
    }
}
