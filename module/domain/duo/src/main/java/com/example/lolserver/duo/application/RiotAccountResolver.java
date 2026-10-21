package com.example.lolserver.duo.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.duo.application.port.out.SummonerStatsPort;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.duo.domain.vo.RiotId;
import com.example.lolserver.member.application.model.readmodel.LinkedRiotAccountReadModel;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiotAccountResolver {

    private final MemberQueryUseCase memberQueryUseCase;
    private final SummonerStatsPort summonerStatsPort;

    public LinkedRiotAccountReadModel resolveLinkedAccount(Long memberId) {
        return memberQueryUseCase.findLinkedRiotAccount(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.RIOT_ACCOUNT_NOT_LINKED));
    }

    /**
     * 듀오 글·신청에 스냅샷으로 넣을 Riot 계정 정보. 듀오는 RSO 연동 회원만 쓰므로 Riot ID 는 연동 때 받은 값이고,
     * lol-repository 에는 통계만 묻는다. 듀오는 lol-repository 없이도 동작해야 한다.
     *
     * <p>통계를 받지 못하면 — 무응답·장애·색인된 적 없는 소환사 — 통계 없이 Riot ID 만 담는다. 이때 티어는 "모름"이라
     * 언랭 검증도 하지 않는다. 연동에 Riot ID 가 없으면(app V4 이전 연동) lol-repository 를 부르기 전에 다시 연동하라고 막는다.
     */
    public RiotAccountStats lookupStats(LinkedRiotAccountReadModel account) {
        RiotId riotId = new RiotId(account.getGameName(), account.getTagLine());
        return summonerStatsPort.findByPuuid(account.getPuuid())
                .map(stats -> RiotAccountStats.of(riotId, stats))
                .orElseGet(() -> RiotAccountStats.unknown(riotId));
    }
}
