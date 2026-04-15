package com.example.lolserver.domain.duo.application;

import com.example.lolserver.QueueType;
import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.example.lolserver.domain.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.domain.duo.domain.vo.TierInfo;
import com.example.lolserver.domain.league.application.port.LeaguePersistencePort;
import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.domain.match.application.port.out.MatchPersistencePort;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.gamedata.ParticipantData;
import com.example.lolserver.domain.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.domain.member.domain.SocialAccount;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
import com.example.lolserver.support.PaginationRequest;
import com.example.lolserver.support.SliceResult;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class RiotAccountResolver {

    private static final int MOST_CHAMPION_LIMIT = 3;
    private static final int RECENT_GAME_SIZE = 20;

    private final MemberPersistencePort memberPersistencePort;
    private final LeaguePersistencePort leaguePersistencePort;
    private final MatchPersistencePort matchPersistencePort;

    public String extractRiotPuuid(Long memberId) {
        Member member = memberPersistencePort.findByIdWithSocialAccounts(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        return member.getSocialAccounts().stream()
                .filter(sa -> OAuthProvider.RIOT.name().equals(sa.getProvider()) && sa.getPuuid() != null)
                .map(SocialAccount::getPuuid)
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.RIOT_ACCOUNT_NOT_LINKED));
    }

    public RiotAccountStats lookupAllStats(String puuid) {
        CompletableFuture<TierInfo> tierFuture =
                CompletableFuture.supplyAsync(() -> lookupTierInfo(puuid));
        CompletableFuture<List<MostChampion>> championsFuture =
                CompletableFuture.supplyAsync(() -> lookupMostChampions(puuid));
        CompletableFuture<RecentGameSummary> recentGameFuture =
                CompletableFuture.supplyAsync(() -> lookupRecentGameSummary(puuid));

        return new RiotAccountStats(
                tierFuture.join(),
                championsFuture.join(),
                recentGameFuture.join()
        );
    }

    public TierInfo lookupTierInfo(String puuid) {
        return leaguePersistencePort.findAllLeaguesByPuuid(puuid).stream()
                .filter(league -> QueueType.RANKED_SOLO_5x5.name().equals(league.getQueue()))
                .findFirst()
                .map(league -> new TierInfo(
                        league.getTier(), league.getRank(), league.getLeaguePoints()))
                .orElse(TierInfo.UNRANKED);
    }

    public List<MostChampion> lookupMostChampions(String puuid) {
        List<MSChampion> rankChampions = matchPersistencePort.getRankChampions(
                puuid, null, QueueType.RANKED_SOLO_5x5.getQueueId());

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
        PaginationRequest paginationRequest = new PaginationRequest(
                0, RECENT_GAME_SIZE, "match", PaginationRequest.SortDirection.DESC);

        SliceResult<GameReadModel> matchResult = matchPersistencePort.getMatches(
                puuid, QueueType.RANKED_SOLO_5x5.getQueueId(), paginationRequest);

        List<GameReadModel> games = matchResult.getContent();
        if (games.isEmpty()) {
            return new RecentGameSummary(0, 0, Collections.emptyList());
        }

        int wins = 0;
        int losses = 0;
        List<RecentGameSummary.PlayedChampion> playedChampions = new ArrayList<>();

        for (GameReadModel game : games) {
            ParticipantData participant = game.getParticipantData().stream()
                    .filter(p -> puuid.equals(p.getPuuid()))
                    .findFirst()
                    .orElse(null);

            if (participant == null) {
                continue;
            }

            if (participant.isWin()) {
                wins++;
            } else {
                losses++;
            }

            playedChampions.add(new RecentGameSummary.PlayedChampion(
                    participant.getChampionId(), participant.getChampionName()));
        }

        return new RecentGameSummary(wins, losses, playedChampions);
    }

    public record RiotAccountStats(
            TierInfo tierInfo,
            List<MostChampion> mostChampions,
            RecentGameSummary recentGameSummary
    ) {
    }
}
