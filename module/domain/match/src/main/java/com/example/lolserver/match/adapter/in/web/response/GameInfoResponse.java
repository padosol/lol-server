package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.domain.gamedata.GameInfoData;

/**
 * 매치 API 응답 - 게임 정보.
 */
public record GameInfoResponse(
        String dataVersion,
        long gameCreation,
        long gameDuration,
        long gameEndTimestamp,
        String gameMode,
        long gameStartTimestamp,
        String gameType,
        String gameVersion,
        int mapId,
        String platformId,
        int queueId,
        String tournamentCode,
        String matchId,
        String averageTier,
        String averageRank
) {
    public static GameInfoResponse from(GameInfoData data) {
        return new GameInfoResponse(
                data.getDataVersion(),
                data.getGameCreation(),
                data.getGameDuration(),
                data.getGameEndTimestamp(),
                data.getGameMode(),
                data.getGameStartTimestamp(),
                data.getGameType(),
                data.getGameVersion(),
                data.getMapId(),
                data.getPlatformId(),
                data.getQueueId(),
                data.getTournamentCode(),
                data.getMatchId(),
                data.getAverageTier(),
                data.getAverageRank()
        );
    }
}
