package com.example.lolserver.riot.dto.match;

import com.example.lolserver.entity.match.Match;
import com.example.lolserver.riot.dto.error.ErrorDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchDto extends ErrorDTO {

    private MetadataDto metadata;
    private InfoDto info;

    public Match toEntity() {
        return Match.builder()
                .matchId(metadata.getMatchId())
                .endOfGameResult(info.getEndOfGameResult())
                .dateVersion(metadata.getDataVersion())
                .gameCreation(info.getGameCreation())
                .gameDuration(info.getGameDuration())
                .gameEndTimestamp(info.getGameEndTimestamp())
                .gameId(info.getGameId())
                .gameMode(info.getGameMode())
                .gameName(info.getGameName())
                .gameStartTimestamp(info.getGameStartTimestamp())
                .gameType(info.getGameType())
                .gameVersion(info.getGameVersion())
                .mapId(info.getMapId())
                .platformId(info.getPlatformId())
                .queueId(info.getQueueId())
                .tournamentCode(info.getTournamentCode())
                .build();
    }
}