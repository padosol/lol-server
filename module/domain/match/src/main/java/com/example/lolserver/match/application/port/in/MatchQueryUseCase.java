package com.example.lolserver.match.application.port.in;

import com.example.lolserver.match.application.command.MSChampionCommand;
import com.example.lolserver.match.application.command.MatchCommand;
import com.example.lolserver.match.application.model.DailyGameCountSummaryReadModel;
import com.example.lolserver.match.application.model.GameReadModel;
import com.example.lolserver.match.application.model.MSChampionByQueueReadModel;
import com.example.lolserver.match.application.model.MSChampionReadModel;
import com.example.lolserver.match.application.model.PlayerMatchReadModel;
import com.example.lolserver.match.application.model.TimelineReadModel;
import com.example.lolserver.common.support.SliceResult;

import java.util.List;

public interface MatchQueryUseCase {

    SliceResult<GameReadModel> getMatches(MatchCommand matchCommand);

    MSChampionByQueueReadModel getRankChampions(MSChampionCommand command);

    /**
     * 다른 컨텍스트에 노출하기 위한 모스트 챔피언 요약 조회.
     */
    List<MSChampionReadModel> getRankChampionSummaries(MSChampionCommand command);

    /**
     * puuid의 최근 {@code size}경기에서 해당 플레이어의 챔피언/승패를 ReadModel로 반환한다.
     * 다른 컨텍스트(duo 등)가 최근 전적 요약을 계산할 때 사용한다.
     */
    List<PlayerMatchReadModel> getRecentPlayerMatches(String puuid, Integer queueId, int size);

    GameReadModel getGameData(String matchId);

    TimelineReadModel getTimelineData(String matchId);

    SliceResult<GameReadModel> getMatchesBatch(MatchCommand matchCommand);

    SliceResult<String> findAllMatchIds(MatchCommand matchCommand);

    DailyGameCountSummaryReadModel getDailyGameCounts(String puuid, Integer season, Integer queueId);
}
