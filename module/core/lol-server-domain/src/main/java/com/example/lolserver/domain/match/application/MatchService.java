package com.example.lolserver.domain.match.application;

import com.example.lolserver.domain.match.application.command.MSChampionCommand;
import com.example.lolserver.domain.match.application.command.MatchCommand;
import com.example.lolserver.domain.match.application.model.DailyGameCountReadModel;
import com.example.lolserver.domain.match.application.model.DailyGameCountSummaryReadModel;
import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.domain.match.domain.MSChampionByQueue;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.domain.match.application.port.in.MatchQueryUseCase;
import com.example.lolserver.domain.match.application.port.out.MatchIdsCachePort;
import com.example.lolserver.domain.match.application.port.out.MatchPersistencePort;
import com.example.lolserver.domain.match.application.port.out.MatchSingleCachePort;
import com.example.lolserver.support.PaginationRequest;
import com.example.lolserver.support.SliceResult;
import com.example.lolserver.support.logging.LogExecutionTime;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public MSChampionByQueue getRankChampions(MSChampionCommand command) {
        return matchPersistencePort.getRankChampions(command.getPuuid(), command.getSeason());
    }

    public GameReadModel getGameData(String matchId) {
        return matchPersistencePort.getGameData(matchId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_MATCH_ID, "존재하지 않는 MatchId 입니다. " + matchId));
    }

    public TimelineData getTimelineData(String matchId) {
        return matchPersistencePort.getTimelineData(matchId);
    }

    @LogExecutionTime
    public SliceResult<GameReadModel> getMatchesBatch(MatchCommand matchCommand) {
        String puuid = matchCommand.getPuuid();
        Integer season = matchCommand.getSeason();
        Integer queueId = matchCommand.getQueueId();
        int pageNo = matchCommand.getPageNo() == null ? 0 : matchCommand.getPageNo();

        // season → epoch ms 변환 헬퍼가 없어 ZSET 범위 조회 시 score 산정 불가.
        // 이 경우 cache 를 우회하고 기존 DB 경로로 fallback 한다.
        if (season != null) {
            return loadFromDbDirect(puuid, season, queueId, pageNo);
        }

        List<String> matchIds = matchIdsCachePort.findIds(puuid, null, null)
                .orElseGet(() -> loadMatchIdsFromDbAndCache(puuid));
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

        Map<String, GameReadModel> matchesById = new HashMap<>(cached);
        if (missingIds.isEmpty()) {
            return matchesById;
        }

        List<GameReadModel> fromDb = matchPersistencePort.findMatchesByIds(missingIds);
        Map<String, GameReadModel> dbMap = new HashMap<>();
        for (GameReadModel game : fromDb) {
            String id = matchIdOf(game);
            if (id != null) {
                dbMap.put(id, game);
            }
        }
        if (!dbMap.isEmpty()) {
            matchSingleCachePort.saveAll(dbMap);
            matchesById.putAll(dbMap);
        }
        return matchesById;
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

    private List<String> loadMatchIdsFromDbAndCache(String puuid) {
        List<String> matchIds = matchPersistencePort.findRecentMatchIds(puuid, DEFAULT_PAGE_SIZE);
        if (matchIds.isEmpty()) {
            return Collections.emptyList();
        }

        // ZSET 저장을 위한 score 산정: DB 매치 헤더로부터 gameCreation 을 가져온다.
        List<GameReadModel> games = matchPersistencePort.findMatchesByIds(matchIds);
        List<Map.Entry<String, Long>> entries = new ArrayList<>(games.size());
        for (GameReadModel game : games) {
            String id = matchIdOf(game);
            Long score = gameCreationOf(game);
            if (id != null && score != null) {
                entries.add(new AbstractMap.SimpleEntry<>(id, score));
            }
        }
        if (!entries.isEmpty()) {
            matchIdsCachePort.saveIds(puuid, entries);
        }
        return matchIds;
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

    private Long gameCreationOf(GameReadModel game) {
        return game.getGameInfoData() == null ? null : game.getGameInfoData().getGameCreation();
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
