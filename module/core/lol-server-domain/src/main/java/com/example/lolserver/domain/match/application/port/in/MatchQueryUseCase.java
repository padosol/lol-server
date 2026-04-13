package com.example.lolserver.domain.match.application.port.in;

import com.example.lolserver.domain.match.application.command.MSChampionCommand;
import com.example.lolserver.domain.match.application.command.MatchCommand;
import com.example.lolserver.domain.match.application.model.DailyGameCountSummaryReadModel;
import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.support.SliceResult;

import java.util.List;

public interface MatchQueryUseCase {

    SliceResult<GameReadModel> getMatches(MatchCommand matchCommand);

    List<MSChampion> getRankChampions(MSChampionCommand command);

    GameReadModel getGameData(String matchId);

    TimelineData getTimelineData(String matchId);

    SliceResult<GameReadModel> getMatchesBatch(MatchCommand matchCommand);

    SliceResult<String> findAllMatchIds(MatchCommand matchCommand);

    DailyGameCountSummaryReadModel getDailyGameCounts(String puuid, Integer season, Integer queueId);
}
