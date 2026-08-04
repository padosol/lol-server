package com.example.lolserver.duo.application;

import com.example.lolserver.shared.QueueType;
import com.example.lolserver.duo.domain.vo.MostChampion;
import com.example.lolserver.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.duo.domain.vo.TierInfo;
import com.example.lolserver.match.application.command.MSChampionCommand;
import com.example.lolserver.match.application.model.readmodel.MSChampionReadModel;
import com.example.lolserver.match.application.model.readmodel.PlayerMatchReadModel;
import com.example.lolserver.match.application.port.in.MatchQueryUseCase;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.summoner.application.port.in.LeagueQueryUseCase;
import com.example.lolserver.summoner.application.port.in.SummonerQueryUseCase;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RiotAccountResolver {

    private static final int MOST_CHAMPION_LIMIT = 3;
    private static final int RECENT_GAME_SIZE = 20;

    private final MemberQueryUseCase memberQueryUseCase;
    private final LeagueQueryUseCase leagueQueryUseCase;
    private final MatchQueryUseCase matchQueryUseCase;
    private final SummonerQueryUseCase summonerQueryUseCase;

    public String extractRiotPuuid(Long memberId) {
        return memberQueryUseCase.findRiotPuuid(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.RIOT_ACCOUNT_NOT_LINKED));
    }

    /**
     * 전적 데이터가 적재된 적 없는 계정을 언랭과 구분해 걸러낸다.
     *
     * <p>리그·매치 조회는 DB 만 보므로, 소환사 자체가 색인된 적 없으면 실제 티어와 무관하게
     * 언랭으로 보인다. 그대로 두면 "언랭이라 등록 불가"라는 잘못된 안내가 나가므로,
     * 이 경우는 소환사 검색을 먼저 하도록 별도 에러로 알린다.
     */
    public void validateSummonerIndexed(String puuid) {
        if (summonerQueryUseCase.findSummonerByPuuid(puuid).isEmpty()) {
            throw new CoreException(ErrorType.SUMMONER_SEARCH_REQUIRED);
        }
    }

    public RiotAccountStats lookupAllStats(String puuid) {
        validateSummonerIndexed(puuid);

        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        CompletableFuture<TierInfo> tierFuture =
                CompletableFuture.supplyAsync(() -> withMdc(contextMap, () -> lookupTierInfo(puuid)));
        CompletableFuture<List<MostChampion>> championsFuture =
                CompletableFuture.supplyAsync(() -> withMdc(contextMap, () -> lookupMostChampions(puuid)));
        CompletableFuture<RecentGameSummary> recentGameFuture =
                CompletableFuture.supplyAsync(() -> withMdc(contextMap, () -> lookupRecentGameSummary(puuid)));

        return new RiotAccountStats(
                join(tierFuture),
                join(championsFuture),
                join(recentGameFuture)
        );
    }

    /**
     * join() 이 씌우는 CompletionException 을 벗겨 원래 예외를 그대로 던진다.
     * 래핑된 채로 올라가면 CoreException 이 전역 핸들러에 잡히지 않아 전부 500 이 된다.
     */
    private <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    private <T> T withMdc(Map<String, String> contextMap, Supplier<T> supplier) {
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
        try {
            return supplier.get();
        } finally {
            MDC.clear();
        }
    }

    public TierInfo lookupTierInfo(String puuid) {
        return leagueQueryUseCase.getLeagueSummariesByPuuid(puuid).stream()
                .filter(league -> QueueType.RANKED_SOLO_5x5.name().equals(league.getQueue()))
                .findFirst()
                .map(league -> new TierInfo(
                        league.getTier(), league.getRank(), league.getLeaguePoints()))
                .orElse(TierInfo.UNRANKED);
    }

    public List<MostChampion> lookupMostChampions(String puuid) {
        MSChampionCommand command = new MSChampionCommand();
        command.setPuuid(puuid);

        List<MSChampionReadModel> rankChampions = matchQueryUseCase.getRankChampionSummaries(command);

        return rankChampions.stream()
                .limit(MOST_CHAMPION_LIMIT)
                .map(mc -> new MostChampion(
                        mc.getChampionId(),
                        mc.getChampionName(),
                        mc.getPlayCount(),
                        mc.getWin(),
                        mc.getLosses()))
                .toList();
    }

    public RecentGameSummary lookupRecentGameSummary(String puuid) {
        List<PlayerMatchReadModel> matches = matchQueryUseCase.getRecentPlayerMatches(
                puuid, QueueType.RANKED_SOLO_5x5.getQueueId(), RECENT_GAME_SIZE);

        if (matches.isEmpty()) {
            return new RecentGameSummary(0, 0, Collections.emptyList());
        }

        int wins = 0;
        int losses = 0;
        List<RecentGameSummary.PlayedChampion> playedChampions = new ArrayList<>();

        for (PlayerMatchReadModel match : matches) {
            if (match.isWin()) {
                wins++;
            } else {
                losses++;
            }

            playedChampions.add(new RecentGameSummary.PlayedChampion(
                    match.getChampionId(), match.getChampionName()));
        }

        return new RecentGameSummary(wins, losses, playedChampions);
    }
}
