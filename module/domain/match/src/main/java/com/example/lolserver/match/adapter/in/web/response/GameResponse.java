package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.application.model.readmodel.GameReadModel;

import java.util.List;

/**
 * 매치 단건/목록 API 응답.
 * 필드명은 기존 GameReadModel 직렬화 형태(gameInfoData/participantData/teamInfoData)를 유지한다.
 */
public record GameResponse(
        GameInfoResponse gameInfoData,
        List<ParticipantResponse> participantData,
        TeamResponse teamInfoData
) {
    public static GameResponse from(GameReadModel model) {
        return new GameResponse(
                model.getGameInfoData() == null ? null : GameInfoResponse.from(model.getGameInfoData()),
                model.getParticipantData() == null ? null
                        : model.getParticipantData().stream().map(ParticipantResponse::from).toList(),
                model.getTeamInfoData() == null ? null : TeamResponse.from(model.getTeamInfoData())
        );
    }
}
