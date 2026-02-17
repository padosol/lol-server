package com.example.lolserver.domain.match.application;

import com.example.lolserver.domain.match.application.command.MSChampionCommand;
import com.example.lolserver.domain.match.application.command.MatchCommand;
import com.example.lolserver.domain.match.domain.GameData;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.domain.match.application.port.out.MatchPersistencePort;
import com.example.lolserver.support.Page;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchPersistencePort matchPersistencePort;

    public Page<GameData> getMatches(MatchCommand matchCommand) {
        Pageable pageable = PageRequest.of(
                matchCommand.getPageNo(), 20, Sort.by(Sort.Direction.DESC, "match"));

        return matchPersistencePort.getMatches(matchCommand.getPuuid(), matchCommand.getQueueId(), pageable);
    }

    public List<MSChampion> getRankChampions(MSChampionCommand command) {
        return matchPersistencePort.getRankChampions(command.getPuuid(), command.getSeason(), command.getQueueId());
    }

    public GameData getGameData(String matchId) {
        return matchPersistencePort.getGameData(matchId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_MATCH_ID, "존재하지 않는 MatchId 입니다. " + matchId));
    }

    public TimelineData getTimelineData(String matchId) {
        return matchPersistencePort.getTimelineData(matchId);
    }

    public Page<GameData> getMatchesBatch(MatchCommand matchCommand) {
        Pageable pageable = PageRequest.of(
                matchCommand.getPageNo(), 20, Sort.by(Sort.Direction.DESC, "match"));

        return matchPersistencePort.getMatchesBatch(matchCommand.getPuuid(), matchCommand.getQueueId(), pageable);
    }

    public Page<String> findAllMatchIds(MatchCommand matchCommand) {
        Pageable pageable = PageRequest.of(
                matchCommand.getPageNo(),
                20,
                Sort.by(Sort.Direction.DESC, "match")
        );
        return matchPersistencePort.findAllMatchIds(matchCommand.getPuuid(), matchCommand.getQueueId(), pageable);
    }
}
