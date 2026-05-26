package com.example.lolserver.duo.application;

import com.example.lolserver.QueueType;
import com.example.lolserver.duo.domain.vo.MostChampion;
import com.example.lolserver.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.duo.domain.vo.TierInfo;
import com.example.lolserver.match.application.command.MSChampionCommand;
import com.example.lolserver.match.application.model.MSChampionReadModel;
import com.example.lolserver.match.application.model.PlayerMatchReadModel;
import com.example.lolserver.match.application.port.in.MatchQueryUseCase;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.summoner.application.port.in.LeagueQueryUseCase;
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
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RiotAccountResolver {

    private static final int MOST_CHAMPION_LIMIT = 3;
    private static final int RECENT_GAME_SIZE = 20;

    private final MemberQueryUseCase memberQueryUseCase;
    private final LeagueQueryUseCase leagueQueryUseCase;
    private final MatchQueryUseCase matchQueryUseCase;

    public String extractRiotPuuid(Long memberId) {
        return memberQueryUseCase.findRiotPuuid(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.RIOT_ACCOUNT_NOT_LINKED));
    }

    public RiotAccountStats lookupAllStats(String puuid) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        CompletableFuture<TierInfo> tierFuture =
                CompletableFuture.supplyAsync(() -> withMdc(contextMap, () -> lookupTierInfo(puuid)));
        CompletableFuture<List<MostChampion>> championsFuture =
                CompletableFuture.supplyAsync(() -> withMdc(contextMap, () -> lookupMostChampions(puuid)));
        CompletableFuture<RecentGameSummary> recentGameFuture =
                CompletableFuture.supplyAsync(() -> withMdc(contextMap, () -> lookupRecentGameSummary(puuid)));

        return new RiotAccountStats(
                tierFuture.join(),
                championsFuture.join(),
                recentGameFuture.join()
        );
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
