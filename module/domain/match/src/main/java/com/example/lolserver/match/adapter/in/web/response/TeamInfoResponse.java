package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.application.model.readmodel.TeamInfoReadModel;

/**
 * 매치 API 응답 - 팀 단건 정보.
 */
public record TeamInfoResponse(
        int teamId,
        boolean win,
        int championKills,
        int baronKills,
        int dragonKills,
        int towerKills,
        int inhibitorKills,
        Integer[] goldTimeline,
        Integer[] timestamps
) {
    public static TeamInfoResponse from(TeamInfoReadModel data) {
        return new TeamInfoResponse(
                data.getTeamId(),
                data.isWin(),
                data.getChampionKills(),
                data.getBaronKills(),
                data.getDragonKills(),
                data.getTowerKills(),
                data.getInhibitorKills(),
                data.getGoldTimeline(),
                data.getTimestamps()
        );
    }
}
