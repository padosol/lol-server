package com.example.lolserver.riot.dto.match;

import com.example.lolserver.entity.match.Match;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchDto {

    private MetadataDto metadata;
    private InfoDto info;

    public Match toEntity() {
        return Match.builder()
                .matchId(metadata.getMatchId())
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
