package com.example.lolserver.match.application;

import com.example.lolserver.match.application.command.MSChampionCommand;
import com.example.lolserver.match.application.command.MatchCommand;
import com.example.lolserver.match.application.model.readmodel.DailyGameCountReadModel;
import com.example.lolserver.match.application.model.readmodel.DailyGameCountSummaryReadModel;
import com.example.lolserver.match.application.model.readmodel.GameReadModel;
import com.example.lolserver.match.application.model.readmodel.MSChampionByQueueReadModel;
import com.example.lolserver.match.application.model.readmodel.MSChampionReadModel;
import com.example.lolserver.match.application.model.readmodel.PlayerMatchReadModel;
import com.example.lolserver.match.application.model.readmodel.TimelineReadModel;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService implements MatchQueryUseCase {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String DEFAULT_SORT_FIELD = "match";
    private static final int FIRST_PAGE_NO = 0;

    /** 전적 목록 DB 쿼리가 노출하는 게임 모드. 오버레이도 같은 제한을 걸어야 목록 구성이 어긋나지 않는다. */
    private static final Set<String> LISTABLE_GAME_MODES = Set.of("CLASSIC", "CHERRY");

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

    /**
     * 전적 목록 조회. DB 조회는 항상 수행하고, 첫 페이지에 한해 캐시 오버레이를 덧씌운다.
     * <p>
     * 캐시 ZSET 은 write 측(lol-repository)에서 20 건으로 trim 되므로 목록 전체의 소스로 쓸 수 없다.
     * season·queueId 필터와 페이징은 DB 가 그대로 책임지고, 캐시는 "갱신 직후 비동기 적재가 아직 안 끝나
     * DB 에는 없고 캐시에만 있는 최신 매치" 만 첫 페이지 위에 얹는 역할을 한다.
     */
    @LogExecutionTime
    public SliceResult<GameReadModel> getMatchesBatch(MatchCommand matchCommand) {
        String puuid = matchCommand.getPuuid();
        Integer season = matchCommand.getSeason();
        Integer queueId = matchCommand.getQueueId();
        int pageNo = matchCommand.getPageNo() == null ? FIRST_PAGE_NO : matchCommand.getPageNo();

        SliceResult<GameReadModel> dbResult = loadFromDbDirect(puuid, season, queueId, pageNo);
        if (pageNo != FIRST_PAGE_NO) {
            return dbResult;
        }

        return overlayCachedMatches(puuid, season, queueId, dbResult);
    }

    private SliceResult<GameReadModel> loadFromDbDirect(
            String puuid, Integer season, Integer queueId, int pageNo) {
        PaginationRequest paginationRequest = new PaginationRequest(
                pageNo, DEFAULT_PAGE_SIZE, DEFAULT_SORT_FIELD, PaginationRequest.SortDirection.DESC);
        return matchPersistencePort.getMatchesBatch(puuid, season, queueId, paginationRequest);
    }

    private SliceResult<GameReadModel> overlayCachedMatches(
            String puuid, Integer season, Integer queueId, SliceResult<GameReadModel> dbResult) {
        List<String> cachedIds = matchIdsCachePort.findIds(puuid).orElseGet(Collections::emptyList);
        if (cachedIds.isEmpty()) {
            return dbResult;
        }

        List<GameReadModel> dbContent = dbResult.getContent();
        Set<String> dbMatchIds = new HashSet<>();
        for (GameReadModel game : dbContent) {
            String matchId = matchIdOf(game);
            if (matchId != null) {
                dbMatchIds.add(matchId);
            }
        }

        List<String> candidateIds = cachedIds.stream()
                .filter(matchId -> !dbMatchIds.contains(matchId))
                .toList();
        if (candidateIds.isEmpty()) {
            return dbResult;
        }

        List<GameReadModel> overlay = collectOverlay(candidateIds, season, queueId, pageFloorOf(dbContent));
        if (overlay.isEmpty()) {
            return dbResult;
        }

        overlay.sort(Comparator.comparingLong(this::orderKeyOf).reversed());

        // DB 첫 페이지 꼬리를 잘라내면 그 매치가 어느 페이지에서도 안 보이므로 자르지 않는다.
        List<GameReadModel> merged = new ArrayList<>(overlay.size() + dbContent.size());
        merged.addAll(overlay);
        merged.addAll(dbContent);
        return new SliceResult<>(merged, dbResult.isHasNext());
    }

    private List<GameReadModel> collectOverlay(
            List<String> candidateIds, Integer season, Integer queueId, long pageFloor) {
        Map<String, GameReadModel> cachedMatches = matchSingleCachePort.findByIds(candidateIds);

        List<GameReadModel> overlay = new ArrayList<>();
        for (String matchId : candidateIds) {
            // 단건 캐시에 없으면 DB 폴백 없이 버린다. DB 에 있는 매치는 이미 DB 페이지가 책임진다.
            GameReadModel game = cachedMatches.get(matchId);
            if (game == null || game.getGameInfoData() == null) {
                continue;
            }
            // DB 첫 페이지 최하단보다 오래된 매치는 2페이지 영역과 겹쳐 중복·순서역전을 만든다.
            if (orderKeyOf(game) <= pageFloor) {
                continue;
            }
            if (!isListableGameMode(game)) {
                continue;
            }
            if (queueId != null && !queueIdMatches(game, queueId)) {
                continue;
            }
            if (season != null && !seasonMatches(game, season)) {
                continue;
            }
            overlay.add(game);
        }
        return overlay;
    }

    /** DB 첫 페이지의 가장 오래된 매치 정렬키. 페이지가 비었으면 오버레이를 제한하지 않는다. */
    private long pageFloorOf(List<GameReadModel> dbContent) {
        long floor = Long.MIN_VALUE;
        boolean found = false;
        for (GameReadModel game : dbContent) {
            if (game.getGameInfoData() == null) {
                continue;
            }
            long orderKey = orderKeyOf(game);
            floor = found ? Math.min(floor, orderKey) : orderKey;
            found = true;
        }
        return floor;
    }

    /** DB 목록은 gameEndTimestamp DESC 로 정렬되므로 오버레이도 같은 키로 비교·정렬한다. */
    private long orderKeyOf(GameReadModel game) {
        return game.getGameInfoData() == null
                ? Long.MIN_VALUE : game.getGameInfoData().getGameEndTimestamp();
    }

    private boolean isListableGameMode(GameReadModel game) {
        String gameMode = game.getGameInfoData().getGameMode();
        return gameMode != null && LISTABLE_GAME_MODES.contains(gameMode.toUpperCase(Locale.ROOT));
    }

    /**
     * 캐시 JSON 에는 season 필드가 없으므로 gameVersion major 로 판별한다.
     * lol-repository 가 MatchEntity.season 을 {@code gameVersion.split("\\.")[0]} 으로 채우므로 DB 와 규칙이 같다.
     * 판별 불가능하면 시즌 필터 결과를 오염시키지 않도록 제외한다.
     */
    private boolean seasonMatches(GameReadModel game, int season) {
        String gameVersion = game.getGameInfoData().getGameVersion();
        if (gameVersion == null || gameVersion.isBlank()) {
            return false;
        }
        int dotIndex = gameVersion.indexOf('.');
        String major = dotIndex < 0 ? gameVersion : gameVersion.substring(0, dotIndex);
        try {
            return Integer.parseInt(major.trim()) == season;
        } catch (NumberFormatException e) {
            return false;
        }
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
