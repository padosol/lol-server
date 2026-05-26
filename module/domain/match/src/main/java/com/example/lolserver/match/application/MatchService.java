package com.example.lolserver.match.application;

import com.example.lolserver.match.application.command.MSChampionCommand;
import com.example.lolserver.match.application.command.MatchCommand;
import com.example.lolserver.match.application.model.DailyGameCountReadModel;
import com.example.lolserver.match.application.model.DailyGameCountSummaryReadModel;
import com.example.lolserver.match.application.model.GameReadModel;
import com.example.lolserver.match.application.model.MSChampionByQueueReadModel;
import com.example.lolserver.match.application.model.MSChampionReadModel;
import com.example.lolserver.match.application.model.PlayerMatchReadModel;
import com.example.lolserver.match.application.model.TimelineReadModel;
import com.example.lolserver.match.application.port.in.MatchQueryUseCase;
import com.example.lolserver.match.application.port.out.MatchIdsCachePort;
import com.example.lolserver.match.application.port.out.MatchPersistencePort;
import com.example.lolserver.match.application.port.out.MatchSingleCachePort;
import com.example.lolserver.common.support.PaginationRequest;
import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.support.logging.LogExecutionTime;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService implements MatchQueryUseCase {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String DEFAULT_SORT_FIELD = "match";

    private final MatchPersistencePort matchPersistencePort;
    private final MatchIdsCachePort matchIdsCachePort;
    private final MatchSingleCachePort matchSingleCachePort;

    public SliceResult<GameReadModel> getMatches(MatchCommand matchCommand) {
        PaginationRequest paginationRequest = new PaginationRequest(
                matchCommand.getPageNo(), DEFAULT_PAGE_SIZE, DEFAULT_SORT_FIELD, PaginationRequest.SortDirection.DESC);

        return matchPersistencePort.getMatches(matchCommand.getPuuid(), matchCommand.getQueueId(), paginationRequest);
    }

    public MSChampionByQueueReadModel getRankChampions(MSChampionCommand command) {
        return matchPersistencePort.getRankChampions(command.getPuuid(), command.getSeason());
    }

    public List<MSChampionReadModel> getRankChampionSummaries(MSChampionCommand command) {
        return matchPersistencePort.getRankChampions(command.getPuuid(), command.getSeason())
                .solo().stream()
                .map(MSChampionReadModel::of)
                .toList();
    }

    public List<PlayerMatchReadModel> getRecentPlayerMatches(String puuid, Integer queueId, int size) {
        PaginationRequest paginationRequest = new PaginationRequest(
                0, size, DEFAULT_SORT_FIELD, PaginationRequest.SortDirection.DESC);

        SliceResult<GameReadModel> matchResult = matchPersistencePort.getMatches(puuid, queueId, paginationRequest);

        List<PlayerMatchReadModel> result = new ArrayList<>();
        for (GameReadModel game : matchResult.getContent()) {
            game.getParticipantData().stream()
                    .filter(participant -> puuid.equals(participant.getPuuid()))
                    .findFirst()
                    .ifPresent(participant -> result.add(PlayerMatchReadModel.builder()
                            .win(participant.isWin())
                            .championId(participant.getChampionId())
                            .championName(participant.getChampionName())
                            .build()));
        }
        return result;
    }

    public GameReadModel getGameData(String matchId) {
        return matchPersistencePort.getGameData(matchId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_MATCH_ID, "존재하지 않는 MatchId 입니다. " + matchId));
    }

    public TimelineReadModel getTimelineData(String matchId) {
        return matchPersistencePort.getTimelineData(matchId);
    }

    @LogExecutionTime
    public SliceResult<GameReadModel> getMatchesBatch(MatchCommand matchCommand) {
        String puuid = matchCommand.getPuuid();
        Integer season = matchCommand.getSeason();
        Integer queueId = matchCommand.getQueueId();
        int pageNo = matchCommand.getPageNo() == null ? 0 : matchCommand.getPageNo();

        if (season != null) {
            return loadFromDbDirect(puuid, season, queueId, pageNo);
        }

        Optional<List<String>> cachedIds = matchIdsCachePort.findIds(puuid);
        if (cachedIds.isEmpty()) {
            return loadFromDbDirect(puuid, null, queueId, pageNo);
        }

        List<String> matchIds = cachedIds.get();
        if (matchIds.isEmpty()) {
            return new SliceResult<>(Collections.emptyList(), false);
        }

        Map<String, GameReadModel> matchesById = resolveMatchesByIds(matchIds);
        List<GameReadModel> ordered = filterAndOrder(matchIds, matchesById, queueId);
        return pageSlice(ordered, pageNo);
    }

    private SliceResult<GameReadModel> loadFromDbDirect(
            String puuid, Integer season, Integer queueId, int pageNo) {
        PaginationRequest paginationRequest = new PaginationRequest(
                pageNo, DEFAULT_PAGE_SIZE, DEFAULT_SORT_FIELD, PaginationRequest.SortDirection.DESC);
        return matchPersistencePort.getMatchesBatch(puuid, season, queueId, paginationRequest);
    }

    private Map<String, GameReadModel> resolveMatchesByIds(List<String> matchIds) {
        Map<String, GameReadModel> cached = matchSingleCachePort.findByIds(matchIds);
        List<String> missingIds = new ArrayList<>();
        for (String id : matchIds) {
            if (!cached.containsKey(id)) {
                missingIds.add(id);
            }
        }

        if (missingIds.isEmpty()) {
            return new HashMap<>(cached);
        }

        Map<String, GameReadModel> matchesById = new HashMap<>(cached);
        matchesById.putAll(fetchFromDb(missingIds));
        return matchesById;
    }

    private Map<String, GameReadModel> fetchFromDb(List<String> ids) {
        List<GameReadModel> games = matchPersistencePort.findMatchesByIds(ids);
        Map<String, GameReadModel> dbMap = new LinkedHashMap<>();
        for (GameReadModel game : games) {
            String id = matchIdOf(game);
            if (id != null) {
                dbMap.put(id, game);
            }
        }
        return dbMap;
    }

    private List<GameReadModel> filterAndOrder(
            List<String> matchIds, Map<String, GameReadModel> matchesById, Integer queueId) {
        List<GameReadModel> ordered = new ArrayList<>(matchIds.size());
        for (String id : matchIds) {
            GameReadModel game = matchesById.get(id);
            if (game == null) {
                continue;
            }
            if (queueId != null && !queueIdMatches(game, queueId)) {
                continue;
            }
            ordered.add(game);
        }
        return ordered;
    }

    private SliceResult<GameReadModel> pageSlice(List<GameReadModel> ordered, int pageNo) {
        int fromIndex = Math.min(pageNo * DEFAULT_PAGE_SIZE, ordered.size());
        int toIndex = Math.min(fromIndex + DEFAULT_PAGE_SIZE, ordered.size());
        List<GameReadModel> pageContent = ordered.subList(fromIndex, toIndex);
        boolean hasNext = toIndex < ordered.size();
        return new SliceResult<>(new ArrayList<>(pageContent), hasNext);
    }

    private boolean queueIdMatches(GameReadModel game, int queueId) {
        if (game.getGameInfoData() == null) {
            return false;
        }
        return game.getGameInfoData().getQueueId() == queueId;
    }

    private String matchIdOf(GameReadModel game) {
        return game.getGameInfoData() == null ? null : game.getGameInfoData().getMatchId();
    }

    public SliceResult<String> findAllMatchIds(MatchCommand matchCommand) {
        PaginationRequest paginationRequest = new PaginationRequest(
                matchCommand.getPageNo(), DEFAULT_PAGE_SIZE, DEFAULT_SORT_FIELD, PaginationRequest.SortDirection.DESC);

        return matchPersistencePort.findAllMatchIds(
                matchCommand.getPuuid(), matchCommand.getQueueId(),
                paginationRequest);
    }

    public DailyGameCountSummaryReadModel getDailyGameCounts(
            String puuid, Integer season, Integer queueId) {
        LocalDateTime startDate = LocalDate.now().minusMonths(3).atStartOfDay();
        List<DailyGameCountReadModel> dailyCounts =
                matchPersistencePort.getDailyGameCounts(puuid, season, queueId, startDate);

        long minCount = dailyCounts.stream()
                .mapToLong(DailyGameCountReadModel::gameCount).min().orElse(0L);
        long maxCount = dailyCounts.stream()
                .mapToLong(DailyGameCountReadModel::gameCount).max().orElse(0L);

        return new DailyGameCountSummaryReadModel(dailyCounts, minCount, maxCount);
    }
}
