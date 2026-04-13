package com.example.lolserver.domain.match.application;

import com.example.lolserver.domain.match.application.command.MSChampionCommand;
import com.example.lolserver.domain.match.application.command.MatchCommand;
import com.example.lolserver.domain.match.application.model.DailyGameCountReadModel;
import com.example.lolserver.domain.match.application.model.DailyGameCountSummaryReadModel;
import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.domain.match.application.port.out.MatchPersistencePort;
import com.example.lolserver.support.Page;
import com.example.lolserver.support.logging.LogExecutionTime;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private final MatchPersistencePort matchPersistencePort;

    public Page<GameReadModel> getMatches(MatchCommand matchCommand) {
        Pageable pageable = PageRequest.of(
                matchCommand.getPageNo(), 20, Sort.by(Sort.Direction.DESC, "match"));

        return matchPersistencePort.getMatches(matchCommand.getPuuid(), matchCommand.getQueueId(), pageable);
    }

    public List<MSChampion> getRankChampions(MSChampionCommand command) {
        return matchPersistencePort.getRankChampions(command.getPuuid(), command.getSeason(), command.getQueueId());
    }

    public GameReadModel getGameData(String matchId) {
        return matchPersistencePort.getGameData(matchId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_MATCH_ID, "존재하지 않는 MatchId 입니다. " + matchId));
    }

    public TimelineData getTimelineData(String matchId) {
        return matchPersistencePort.getTimelineData(matchId);
    }

    @LogExecutionTime
    public Page<GameReadModel> getMatchesBatch(MatchCommand matchCommand) {
        Pageable pageable = PageRequest.of(
                matchCommand.getPageNo(), 20, Sort.by(Sort.Direction.DESC, "match"));

        return matchPersistencePort.getMatchesBatch(
                matchCommand.getPuuid(), matchCommand.getSeason(), matchCommand.getQueueId(), pageable);
    }

    public Page<String> findAllMatchIds(MatchCommand matchCommand) {
        Pageable pageable = PageRequest.of(
                matchCommand.getPageNo(),
                20,
                Sort.by(Sort.Direction.DESC, "match")
        );
        return matchPersistencePort.findAllMatchIds(matchCommand.getPuuid(), matchCommand.getQueueId(), pageable);
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
