package com.example.lolserver.duo.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.duo.application.port.out.RiotAccountStatsPort;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.member.application.model.readmodel.LinkedRiotAccountReadModel;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiotAccountResolver {

    private final MemberQueryUseCase memberQueryUseCase;
    private final RiotAccountStatsPort riotAccountStatsPort;

    public LinkedRiotAccountReadModel resolveLinkedAccount(Long memberId) {
        return memberQueryUseCase.findLinkedRiotAccount(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.RIOT_ACCOUNT_NOT_LINKED));
    }

    /**
     * 듀오 글·신청에 스냅샷으로 넣을 Riot 계정 정보. 듀오는 lol-repository 없이도 동작해야 한다.
     *
     * <p>lol-repository 가 통계를 주면 그 값(Riot ID 가 비면 연동 시점 값으로 채움), 주지 못하면 — 무응답·장애·
     * 색인된 적 없는 소환사 — 통계 없이 연동 시점 Riot ID 만 담는다. 이때 티어는 "모름"이라 언랭 검증도 하지 않는다.
     */
    public RiotAccountStats lookupStats(LinkedRiotAccountReadModel account) {
        return riotAccountStatsPort.findByPuuid(account.getPuuid())
                .map(stats -> stats.withRiotIdFallback(account.getGameName(), account.getTagLine()))
                .orElseGet(() -> RiotAccountStats.unknown(account.getGameName(), account.getTagLine()));
    }
}
